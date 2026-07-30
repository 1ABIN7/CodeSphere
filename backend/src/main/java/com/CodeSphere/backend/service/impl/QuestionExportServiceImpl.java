package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.RubricRepository;
import com.CodeSphere.backend.service.QuestionExportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionExportServiceImpl implements QuestionExportService {

    private final ObjectMapper objectMapper;
    private final QuestionBankRepository questionBankRepository;
    private final RubricRepository rubricRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportQuestions(List<Question> questions, String format) {
        if ("json".equalsIgnoreCase(format)) {
            return exportToJson(questions);
        } else if ("csv".equalsIgnoreCase(format)) {
            return exportToCsv(questions);
        } else {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }

    private byte[] exportToJson(List<Question> questions) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(questions);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JSON export payload", e);
        }
    }

    private byte[] exportToCsv(List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        // JSON-valued columns keep choices, task tests, and reading sub-questions intact
        // while still making the file editable in Excel/Google Sheets.
        sb.append("id,title,content,category,type,questionType,difficulty,tagsJson,optionsJson,correctAnswers,points,negativeScore,codingProblemId,sqlSetup,sqlTestCases,apiTestCases,minWordCount,maxWordCount,passageText,readingDurationSeconds,subQuestionsJson,rubricJson\n");
        for (Question q : questions) {
            // Re-fetch inside this transaction so lazy reading-comprehension children are
            // available even when the controller supplied a detached question list.
            Question source = q.getId() == null ? q : questionBankRepository.findById(q.getId()).orElse(q);
            sb.append(row(
                    value(source.getId()), source.getTitle(), source.getContent(), source.getCategory(), source.getType(), source.getQuestionType(), source.getDifficulty(),
                    json(source.getTags()), json(source.getOptions()), source.getCorrectAnswers(), value(source.getPoints()), value(source.getNegativeScore()), value(source.getCodingProblemId()),
                    source.getSqlSetup(), source.getSqlTestCases(), source.getApiTestCases(), value(source.getMinWordCount()), value(source.getMaxWordCount()),
                    source.getPassageText(), value(source.getReadingDurationSeconds()), json(source.getSubQuestions()), rubricJson(source.getId())
            )).append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value == null ? List.of() : value); }
        catch (Exception exception) { throw new IllegalStateException("Unable to export question data", exception); }
    }

    private String rubricJson(Long questionId) {
        if (questionId == null) return "[]";
        return json(rubricRepository.findByQuestionId(questionId).map(rubric -> rubric.getCriteria().stream()
                .map(criterion -> java.util.Map.of("criterionName", criterion.getCriterionName(), "maxPoints", criterion.getMaxPoints()))
                .toList()).orElse(List.of()));
    }

    private String value(Object value) { return value == null ? "" : String.valueOf(value); }

    private String row(String... values) {
        return java.util.Arrays.stream(values).map(this::escapeCsvField).collect(java.util.stream.Collectors.joining(","));
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        String clean = field.trim();
        // If field contains quotes, commas, or line breaks, escape them per RFC 4180
        if (clean.contains(",") || clean.contains("\"") || clean.contains("\n") || clean.contains("\r")) {
            return "\"" + clean.replace("\"", "\"\"") + "\"";
        }
        return clean;
    }
}
