package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.problem.*;
import com.CodeSphere.backend.dto.common.PageResponse;
import com.CodeSphere.backend.model.*;
import com.CodeSphere.backend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing coding problems and their test cases.
 *
 * Provides CRUD operations, search, filtering, pagination,
 * and statistics tracking for the problem bank.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final QuestionBankRepository questionBankRepository;
    private final ObjectMapper objectMapper;

    // ---- Problem CRUD ----

    /**
     * Create a new coding problem with optional test cases.
     */
    @Transactional
    public ProblemResponse createProblem(ProblemRequest request, Long createdBy) {
        if (problemRepository.existsByTitle(request.getTitle())) {
            throw new IllegalStateException("A problem with this title already exists");
        }

        Problem problem = Problem.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .inputFormat(request.getInputFormat())
                .outputFormat(request.getOutputFormat())
                .constraints(request.getConstraints())
                .difficulty(Difficulty.valueOf(request.getDifficulty().toUpperCase()))
                .timeLimit(request.getTimeLimit() != null ? request.getTimeLimit() : 1000)
                .memoryLimit(request.getMemoryLimit() != null ? request.getMemoryLimit() : 262144)
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .hints(request.getHints() != null ? request.getHints() : new ArrayList<>())
                .editorial(request.getEditorial())
                .editorialCode(request.getEditorialCode())
                .starterCode(request.getStarterCode() != null ? request.getStarterCode() : Map.of())
                .companyTags(request.getCompanyTags() != null ? request.getCompanyTags() : new ArrayList<>())
                .createdBy(createdBy)
                .isPublished(false)
                .build();

        problem = problemRepository.save(problem);

        // Add test cases if provided
        if (request.getTestCases() != null && !request.getTestCases().isEmpty()) {
            addTestCasesToProblem(problem, request.getTestCases());
        }

        createQuestionBankEntry(problem, request.getCategory(), request.getPoints(), request.getNegativeScore());

        log.info("[ProblemService] Created problem: {} (ID: {})", problem.getTitle(), problem.getId());
        return mapToResponse(problem, true);
    }

    private void createQuestionBankEntry(Problem problem, String category, Integer points, Integer negativeScore) {
        if (questionBankRepository.existsByCodingProblemId(problem.getId())) return;
        boolean debugging = problem.getTags() != null && problem.getTags().stream()
                .anyMatch(tag -> "debugging".equalsIgnoreCase(tag));
        Question question = new Question();
        question.setTitle(problem.getTitle());
        question.setContent(problem.getDescription());
        question.setCategory(category == null || category.isBlank() ? (debugging ? "Debugging" : "Coding practice") : category.trim());
        question.setType(debugging ? "DEBUGGING" : "CODING");
        question.setQuestionType(debugging ? "DEBUGGING" : "CODING");
        question.setDifficulty(problem.getDifficulty().name());
        question.setTags(problem.getTags() == null ? new ArrayList<>() : new ArrayList<>(problem.getTags()));
        question.setCorrectAnswers("");
        question.setPoints(points != null && points >= 0 ? points : pointsFor(problem.getDifficulty()));
        question.setNegativeScore(negativeScore != null && negativeScore >= 0 ? negativeScore : 0);
        question.setStatus(ApprovalStatus.APPROVED);
        question.setCodingProblemId(problem.getId());
        question.setSystemGenerated(true);
        questionBankRepository.save(question);
    }

    private int pointsFor(Difficulty difficulty) {
        return switch (difficulty) {
            case HARD -> 15;
            case MEDIUM -> 10;
            default -> 5;
        };
    }

    @Transactional(readOnly = true)
    public String exportTasksCsv() {
        StringBuilder csv = new StringBuilder("title,taskType,description,inputFormat,outputFormat,constraints,difficulty,category,tags,points,negativeScore,timeLimit,memoryLimit,testCases\n");
        for (Problem problem : problemRepository.findAll()) {
            boolean debugging = problem.getTags() != null && problem.getTags().stream().anyMatch(tag -> "debugging".equalsIgnoreCase(tag));
            Question linked = questionBankRepository.findAll().stream().filter(question -> problem.getId().equals(question.getCodingProblemId())).findFirst().orElse(null);
            List<TestCaseRequest> testCases = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problem.getId()).stream().map(testCase -> TestCaseRequest.builder()
                    .inputData(testCase.getInputData()).expectedOutput(testCase.getExpectedOutput()).isSample(testCase.getIsSample())
                    .explanation(testCase.getExplanation()).orderIndex(testCase.getOrderIndex()).timeLimitOverride(testCase.getTimeLimitOverride()).scoreWeight(testCase.getScoreWeight()).build()).toList();
            try {
                csv.append(csvRow(problem.getTitle(), debugging ? "DEBUGGING" : "CODING", problem.getDescription(), problem.getInputFormat(), problem.getOutputFormat(), problem.getConstraints(),
                        problem.getDifficulty().name(), linked == null ? "" : linked.getCategory(), String.join(",", problem.getTags() == null ? List.of() : problem.getTags()),
                        linked == null ? "" : String.valueOf(linked.getPoints()), linked == null ? "" : String.valueOf(linked.getNegativeScore()), String.valueOf(problem.getTimeLimit()), String.valueOf(problem.getMemoryLimit()), objectMapper.writeValueAsString(testCases))).append('\n');
            } catch (IOException exception) { throw new IllegalStateException("Unable to export task test cases", exception); }
        }
        return csv.toString();
    }

    @Transactional
    public int importTasksCsv(MultipartFile file, Long createdBy) {
        try {
            List<List<String>> rows = parseCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
            if (rows.size() < 2) return 0;
            List<String> headers = rows.get(0);
            int imported = 0;
            for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
                List<String> row = rows.get(rowIndex);
                String title = value(headers, row, "title");
                if (title.isBlank()) continue;
                ProblemRequest request = ProblemRequest.builder()
                        .title(title).description(value(headers, row, "description")).inputFormat(value(headers, row, "inputFormat"))
                        .outputFormat(value(headers, row, "outputFormat")).constraints(value(headers, row, "constraints"))
                        .difficulty(blankDefault(value(headers, row, "difficulty"), "MEDIUM")).category(value(headers, row, "category"))
                        .tags(splitTags(value(headers, row, "tags"), value(headers, row, "taskType"))).points(integerValue(value(headers, row, "points")))
                        .negativeScore(integerValue(value(headers, row, "negativeScore"))).timeLimit(integerValue(value(headers, row, "timeLimit")))
                        .memoryLimit(integerValue(value(headers, row, "memoryLimit"))).testCases(readTestCases(value(headers, row, "testCases"))).build();
                createProblem(request, createdBy);
                imported++;
            }
            return imported;
        } catch (IOException exception) { throw new IllegalArgumentException("Unable to read the Task Center CSV", exception); }
    }

    private List<TestCaseRequest> readTestCases(String json) throws IOException { return json == null || json.isBlank() ? List.of() : objectMapper.readValue(json, new TypeReference<List<TestCaseRequest>>() {}); }
    private List<String> splitTags(String tags, String taskType) { List<String> values = new ArrayList<>(Arrays.stream(tags.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList()); if ("DEBUGGING".equalsIgnoreCase(taskType) && values.stream().noneMatch(value -> "debugging".equalsIgnoreCase(value))) values.add("debugging"); return values; }
    private Integer integerValue(String value) { try { return value == null || value.isBlank() ? null : Integer.valueOf(value); } catch (NumberFormatException exception) { return null; } }
    private String blankDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private String value(List<String> headers, List<String> row, String header) { int index = headers.indexOf(header); return index >= 0 && index < row.size() ? row.get(index) : ""; }
    private String csvRow(String... values) { return Arrays.stream(values).map(value -> '"' + (value == null ? "" : value.replace("\"", "\"\"")) + '"').collect(Collectors.joining(",")); }
    private List<List<String>> parseCsv(String csv) { List<List<String>> rows = new ArrayList<>(); List<String> row = new ArrayList<>(); StringBuilder cell = new StringBuilder(); boolean quoted = false; for (int index = 0; index < csv.length(); index++) { char character = csv.charAt(index); if (character == '"' && quoted && index + 1 < csv.length() && csv.charAt(index + 1) == '"') { cell.append('"'); index++; } else if (character == '"') quoted = !quoted; else if (character == ',' && !quoted) { row.add(cell.toString()); cell.setLength(0); } else if ((character == '\n' || character == '\r') && !quoted) { if (character == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') index++; row.add(cell.toString()); if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row); row = new ArrayList<>(); cell.setLength(0); } else cell.append(character); } row.add(cell.toString()); if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row); return rows; }

    /**
     * Update an existing problem.
     */
    @Transactional
    public ProblemResponse updateProblem(Long id, ProblemRequest request) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Problem not found with ID: " + id));

        problem.setTitle(request.getTitle());
        problem.setDescription(request.getDescription());
        problem.setInputFormat(request.getInputFormat());
        problem.setOutputFormat(request.getOutputFormat());
        problem.setConstraints(request.getConstraints());
        problem.setDifficulty(Difficulty.valueOf(request.getDifficulty().toUpperCase()));

        if (request.getTimeLimit() != null) problem.setTimeLimit(request.getTimeLimit());
        if (request.getMemoryLimit() != null) problem.setMemoryLimit(request.getMemoryLimit());
        if (request.getTags() != null) problem.setTags(request.getTags());
        if (request.getHints() != null) problem.setHints(request.getHints());
        if (request.getEditorial() != null) problem.setEditorial(request.getEditorial());
        if (request.getEditorialCode() != null) problem.setEditorialCode(request.getEditorialCode());
        if (request.getStarterCode() != null) problem.setStarterCode(request.getStarterCode());
        if (request.getCompanyTags() != null) problem.setCompanyTags(request.getCompanyTags());

        problem = problemRepository.save(problem);

        log.info("[ProblemService] Updated problem: {} (ID: {})", problem.getTitle(), problem.getId());
        return mapToResponse(problem, true);
    }

    /**
     * Delete a problem and all its test cases.
     */
    @Transactional
    public void deleteProblem(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Problem not found with ID: " + id));

        problemRepository.delete(problem);
        log.info("[ProblemService] Deleted problem: {} (ID: {})", problem.getTitle(), id);
    }

    /**
     * Get a single problem by ID with its sample test cases.
     */
    public ProblemResponse getProblemById(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Problem not found with ID: " + id));

        return mapToResponse(problem, false);
    }

    /**
     * Get a single problem by ID with ALL test cases (admin view).
     */
    public ProblemResponse getProblemByIdAdmin(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Problem not found with ID: " + id));

        return mapToResponse(problem, true);
    }

    /**
     * Publish or unpublish a problem.
     */
    @Transactional
    public ProblemResponse togglePublish(Long id, boolean publish) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Problem not found with ID: " + id));

        // Require at least one test case before publishing
        if (publish) {
            long testCaseCount = testCaseRepository.countByProblemId(id);
            if (testCaseCount == 0) {
                throw new IllegalStateException("Cannot publish a problem with no test cases");
            }
        }

        problem.setIsPublished(publish);
        problem = problemRepository.save(problem);

        log.info("[ProblemService] {} problem: {} (ID: {})",
                publish ? "Published" : "Unpublished", problem.getTitle(), id);
        return mapToResponse(problem, true);
    }

    // ---- Problem Listing & Search ----

    /**
     * List published problems with filtering and pagination.
     */
    public PageResponse<ProblemListResponse> listProblems(
            String difficulty, String search, String sortBy, int page, int size, Long currentUserId) {

        Pageable pageable = PageRequest.of(page, size, getSort(sortBy));
        Page<Problem> problemPage;

        if (search != null && !search.isBlank() && difficulty != null && !difficulty.isBlank()) {
            problemPage = problemRepository.searchPublishedByDifficulty(
                    search, Difficulty.valueOf(difficulty.toUpperCase()), pageable);
        } else if (search != null && !search.isBlank()) {
            problemPage = problemRepository.searchPublished(search, pageable);
        } else if (difficulty != null && !difficulty.isBlank()) {
            problemPage = problemRepository.findByDifficultyAndIsPublishedTrue(
                    Difficulty.valueOf(difficulty.toUpperCase()), pageable);
        } else {
            problemPage = problemRepository.findByIsPublishedTrue(pageable);
        }

        Page<ProblemListResponse> responsePage = problemPage.map(p -> mapToListResponse(p, currentUserId));
        return PageResponse.of(responsePage);
    }

    // ---- Test Case Management ----

    /**
     * Add a test case to a problem.
     */
    @Transactional
    public TestCaseResponse addTestCase(Long problemId, TestCaseRequest request) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new NoSuchElementException("Problem not found with ID: " + problemId));

        TestCase testCase = TestCase.builder()
                .problem(problem)
                .inputData(request.getInputData())
                .expectedOutput(request.getExpectedOutput())
                .isSample(request.getIsSample() != null ? request.getIsSample() : false)
                .explanation(request.getExplanation())
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() :
                        (int) testCaseRepository.countByProblemId(problemId))
                .scoreWeight(request.getScoreWeight() != null ? request.getScoreWeight() : 1)
                .timeLimitOverride(request.getTimeLimitOverride())
                .build();

        testCase = testCaseRepository.save(testCase);
        return mapTestCaseToResponse(testCase, true);
    }

    /**
     * Update an existing test case.
     */
    @Transactional
    public TestCaseResponse updateTestCase(Long problemId, Long testCaseId, TestCaseRequest request) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new NoSuchElementException("Test case not found with ID: " + testCaseId));

        if (!testCase.getProblem().getId().equals(problemId)) {
            throw new IllegalArgumentException("Test case does not belong to this problem");
        }

        testCase.setInputData(request.getInputData());
        testCase.setExpectedOutput(request.getExpectedOutput());
        if (request.getIsSample() != null) testCase.setIsSample(request.getIsSample());
        if (request.getExplanation() != null) testCase.setExplanation(request.getExplanation());
        if (request.getOrderIndex() != null) testCase.setOrderIndex(request.getOrderIndex());
        if (request.getScoreWeight() != null) testCase.setScoreWeight(request.getScoreWeight());
        testCase.setTimeLimitOverride(request.getTimeLimitOverride());

        testCase = testCaseRepository.save(testCase);
        return mapTestCaseToResponse(testCase, true);
    }

    /**
     * Delete a test case.
     */
    @Transactional
    public void deleteTestCase(Long problemId, Long testCaseId) {
        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new NoSuchElementException("Test case not found with ID: " + testCaseId));

        if (!testCase.getProblem().getId().equals(problemId)) {
            throw new IllegalArgumentException("Test case does not belong to this problem");
        }

        testCaseRepository.delete(testCase);
    }

    /**
     * Get test cases for a problem. Admin sees all; candidates see only samples.
     */
    public List<TestCaseResponse> getTestCases(Long problemId, boolean isAdmin) {
        List<TestCase> testCases;
        if (isAdmin) {
            testCases = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId);
        } else {
            testCases = testCaseRepository.findByProblemIdAndIsSampleTrueOrderByOrderIndexAsc(problemId);
        }

        return testCases.stream()
                .map(tc -> mapTestCaseToResponse(tc, isAdmin))
                .collect(Collectors.toList());
    }

    // ---- Internal Helpers ----

    private void addTestCasesToProblem(Problem problem, List<TestCaseRequest> testCaseRequests) {
        for (int i = 0; i < testCaseRequests.size(); i++) {
            TestCaseRequest req = testCaseRequests.get(i);
            TestCase tc = TestCase.builder()
                    .problem(problem)
                    .inputData(req.getInputData())
                    .expectedOutput(req.getExpectedOutput())
                    .isSample(req.getIsSample() != null ? req.getIsSample() : false)
                    .explanation(req.getExplanation())
                    .orderIndex(req.getOrderIndex() != null ? req.getOrderIndex() : i)
                    .scoreWeight(req.getScoreWeight() != null ? req.getScoreWeight() : 1)
                    .timeLimitOverride(req.getTimeLimitOverride())
                    .build();
            testCaseRepository.save(tc);
        }
    }

    private Sort getSort(String sortBy) {
        if (sortBy == null) return Sort.by(Sort.Direction.ASC, "id");
        return switch (sortBy.toLowerCase()) {
            case "difficulty" -> Sort.by(Sort.Direction.ASC, "difficulty");
            case "acceptance" -> Sort.by(Sort.Direction.DESC, "acceptanceRate");
            case "popular" -> Sort.by(Sort.Direction.DESC, "totalSubmissions");
            case "newest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "title" -> Sort.by(Sort.Direction.ASC, "title");
            default -> Sort.by(Sort.Direction.ASC, "id");
        };
    }

    private ProblemResponse mapToResponse(Problem p, boolean includeAllTestCases) {
        List<TestCaseResponse> sampleTestCases;
        if (includeAllTestCases) {
            sampleTestCases = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(p.getId())
                    .stream()
                    .map(tc -> mapTestCaseToResponse(tc, true))
                    .collect(Collectors.toList());
        } else {
            sampleTestCases = testCaseRepository.findByProblemIdAndIsSampleTrueOrderByOrderIndexAsc(p.getId())
                    .stream()
                    .map(tc -> mapTestCaseToResponse(tc, false))
                    .collect(Collectors.toList());
        }

        return ProblemResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .description(p.getDescription())
                .inputFormat(p.getInputFormat())
                .outputFormat(p.getOutputFormat())
                .constraints(p.getConstraints())
                .difficulty(p.getDifficulty().name())
                .timeLimit(p.getTimeLimit())
                .memoryLimit(p.getMemoryLimit())
                .tags(p.getTags())
                .companyTags(p.getCompanyTags())
                .hints(p.getHints())
                .editorial(p.getEditorial())
                .editorialCode(p.getEditorialCode())
                .starterCode(p.getStarterCode())
                .acceptanceRate(p.getAcceptanceRate())
                .totalSubmissions(p.getTotalSubmissions())
                .acceptedSubmissions(p.getAcceptedSubmissions())
                .isPublished(p.getIsPublished())
                .createdBy(p.getCreatedBy())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .sampleTestCases(sampleTestCases)
                .totalTestCases((int) testCaseRepository.countByProblemId(p.getId()))
                .build();
    }

    private ProblemListResponse mapToListResponse(Problem p, Long currentUserId) {
        boolean solved = false;
        if (currentUserId != null) {
            solved = submissionRepository.existsByProblemIdAndUserIdAndStatus(
                    p.getId(), currentUserId, SubmissionStatus.ACCEPTED);
        }

        return ProblemListResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .difficulty(p.getDifficulty().name())
                .acceptanceRate(p.getAcceptanceRate())
                .totalSubmissions(p.getTotalSubmissions())
                .tags(p.getTags())
                .isPublished(p.getIsPublished())
                .solvedByCurrentUser(solved)
                .build();
    }

    private TestCaseResponse mapTestCaseToResponse(TestCase tc, boolean showAll) {
        TestCaseResponse.TestCaseResponseBuilder builder = TestCaseResponse.builder()
                .id(tc.getId())
                .isSample(tc.getIsSample())
                .orderIndex(tc.getOrderIndex())
                .scoreWeight(tc.getScoreWeight());

        // Always show input/output for sample test cases or admin views
        if (tc.getIsSample() || showAll) {
            builder.inputData(tc.getInputData())
                   .expectedOutput(tc.getExpectedOutput())
                   .explanation(tc.getExplanation());
        }

        return builder.build();
    }
}
