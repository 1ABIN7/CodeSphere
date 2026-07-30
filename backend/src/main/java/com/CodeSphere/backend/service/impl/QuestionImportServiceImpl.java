package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.model.Rubric;
import com.CodeSphere.backend.model.RubricCriterion;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.RubricRepository;
import com.CodeSphere.backend.service.QuestionImportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionImportServiceImpl implements QuestionImportService {

    private final QuestionBankRepository repository;
    private final RubricRepository rubricRepository;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @Override
    public List<Question> importBulkQuestions(MultipartFile file, boolean updateExisting) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

        // 1. Archive the raw import file into MinIO object storage vault
        try {
            boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload import log to MinIO vault", e);
        }

        // 2. Identify type and process
        List<ImportedQuestion> parsedQuestions;
        String extension = getFileExtension(file.getOriginalFilename());

        try {
            if ("json".equalsIgnoreCase(extension)) {
                parsedQuestions = objectMapper.readValue(file.getBytes(), new TypeReference<List<Question>>() {}).stream()
                        .map(question -> new ImportedQuestion(question, "")).toList();
            } else if ("csv".equalsIgnoreCase(extension)) {
                parsedQuestions = parseCsv(file);
            } else {
                throw new IllegalArgumentException("Unsupported file type. Only JSON and CSV files are accepted.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing file structure: " + e.getMessage(), e);
        }

        // 3. Preserve the stable exported ID only when the admin selected Update existing.
        List<Question> saved = new ArrayList<>();
        for (ImportedQuestion row : parsedQuestions) {
            Question imported = row.question();
            Question target = updateExisting && imported.getId() != null
                    ? repository.findById(imported.getId()).orElseGet(Question::new)
                    : new Question();
            copyEditableFields(imported, target);
            target = repository.save(target);
            saveRubric(target, row.rubricJson());
            saved.add(target);
        }
        return saved;
    }

    private List<ImportedQuestion> parseCsv(MultipartFile file) throws Exception {
        List<List<String>> rows = parseCsvRows(new String(file.getBytes(), StandardCharsets.UTF_8));
        if (rows.isEmpty()) return List.of();
        List<String> headers = rows.get(0);
        List<ImportedQuestion> questions = new ArrayList<>();
        for (int index = 1; index < rows.size(); index++) {
            Map<String, String> row = row(headers, rows.get(index));
            if (field(row, "title").isBlank()) continue;
            Question question = new Question();
            question.setId(longValue(field(row, "id")));
            question.setTitle(field(row, "title")); question.setContent(field(row, "content"));
            question.setCategory(field(row, "category")); question.setType(field(row, "type"));
            question.setQuestionType(blankDefault(field(row, "questionType"), question.getType())); question.setDifficulty(field(row, "difficulty"));
            // Backward-compatible simple CSV files use a semicolon-delimited Tags column.
            question.setTags(jsonList(field(row, "tagsJson"), field(row, "tags")));
            question.setOptions(jsonList(field(row, "optionsJson"), "")); question.setCorrectAnswers(field(row, "correctAnswers"));
            question.setPoints(integerValue(field(row, "points"), 0)); question.setNegativeScore(integerValue(field(row, "negativeScore"), 0));
            question.setCodingProblemId(longValue(field(row, "codingProblemId"))); question.setSqlSetup(field(row, "sqlSetup"));
            question.setSqlTestCases(field(row, "sqlTestCases")); question.setApiTestCases(field(row, "apiTestCases"));
            question.setMinWordCount(integerValue(field(row, "minWordCount"), 0)); question.setMaxWordCount(integerValue(field(row, "maxWordCount"), 2000));
            question.setPassageText(field(row, "passageText")); question.setReadingDurationSeconds(integerValue(field(row, "readingDurationSeconds"), 0));
            question.setSubQuestions(subQuestions(field(row, "subQuestionsJson")));
            questions.add(new ImportedQuestion(question, field(row, "rubricJson")));
        }
        return questions;
    }

    private void copyEditableFields(Question source, Question target) {
        target.setTitle(source.getTitle()); target.setContent(source.getContent()); target.setCategory(source.getCategory());
        target.setType(source.getType()); target.setQuestionType(source.getQuestionType()); target.setDifficulty(source.getDifficulty());
        target.setTags(source.getTags()); target.setOptions(source.getOptions()); target.setCorrectAnswers(source.getCorrectAnswers());
        target.setPoints(source.getPoints()); target.setNegativeScore(source.getNegativeScore()); target.setCodingProblemId(source.getCodingProblemId());
        target.setSqlSetup(source.getSqlSetup()); target.setSqlTestCases(source.getSqlTestCases()); target.setApiTestCases(source.getApiTestCases());
        target.setMinWordCount(source.getMinWordCount()); target.setMaxWordCount(source.getMaxWordCount()); target.setPassageText(source.getPassageText());
        target.setReadingDurationSeconds(source.getReadingDurationSeconds()); target.setSubQuestions(source.getSubQuestions());
    }

    private void saveRubric(Question question, String rubricJson) {
        if (rubricJson == null || rubricJson.isBlank()) return;
        try {
            List<RubricData> values = objectMapper.readValue(rubricJson, new TypeReference<List<RubricData>>() {});
            Rubric rubric = rubricRepository.findByQuestionId(question.getId()).orElseGet(Rubric::new);
            rubric.setQuestion(question);
            rubric.getCriteria().clear();
            values.stream().filter(value -> value.criterionName() != null && !value.criterionName().isBlank() && value.maxPoints() != null)
                    .forEach(value -> { RubricCriterion criterion = new RubricCriterion(); criterion.setCriterionName(value.criterionName()); criterion.setMaxPoints(value.maxPoints()); rubric.addCriterion(criterion); });
            rubricRepository.save(rubric);
        } catch (Exception exception) { throw new IllegalArgumentException("rubricJson must be valid JSON."); }
    }

    private List<Question> subQuestions(String value) {
        if (value == null || value.isBlank() || "[]".equals(value)) return new ArrayList<>();
        try {
            List<Question> children = objectMapper.readValue(value, new TypeReference<List<Question>>() {});
            children.forEach(child -> child.setId(null));
            return children;
        } catch (Exception exception) { throw new IllegalArgumentException("subQuestionsJson must be valid JSON."); }
    }

    private List<String> jsonList(String json, String legacy) {
        try {
            if (json != null && !json.isBlank()) return objectMapper.readValue(json, new TypeReference<List<String>>() {});
            if (legacy == null || legacy.isBlank()) return new ArrayList<>();
            return java.util.Arrays.stream(legacy.split(";")).map(String::trim).filter(value -> !value.isBlank()).toList();
        } catch (Exception exception) { throw new IllegalArgumentException("Question CSV contains invalid JSON list data."); }
    }

    private Map<String, String> row(List<String> headers, List<String> values) { Map<String, String> output = new LinkedHashMap<>(); for (int index = 0; index < headers.size(); index++) output.put(headers.get(index), index < values.size() ? values.get(index) : ""); return output; }
    private String field(Map<String, String> row, String name) { return row.getOrDefault(name, "").trim(); }
    private String blankDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private Integer integerValue(String value, int fallback) { try { return value == null || value.isBlank() ? fallback : Integer.parseInt(value); } catch (NumberFormatException exception) { return fallback; } }
    private Long longValue(String value) { try { return value == null || value.isBlank() ? null : Long.parseLong(value); } catch (NumberFormatException exception) { return null; } }
    private List<List<String>> parseCsvRows(String csv) { List<List<String>> rows = new ArrayList<>(); List<String> row = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false; for (int index = 0; index < csv.length(); index++) { char character = csv.charAt(index); if (character == '"' && quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') { cell.append('"'); index++; } else if (character == '"') quoted = !quoted; else if (character == ',' && !quoted) { row.add(cell.toString()); cell.setLength(0); } else if ((character == '\n' || character == '\r') && !quoted) { if (character == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') index++; row.add(cell.toString()); if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row); row = new ArrayList<>(); cell.setLength(0); } else cell.append(character); } row.add(cell.toString()); if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row); return rows; }

    private String getFileExtension(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf(".") + 1);
    }

    private record ImportedQuestion(Question question, String rubricJson) {}
    private record RubricData(String criterionName, Integer maxPoints) {}
}
