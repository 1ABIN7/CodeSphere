package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.problem.*;
import com.CodeSphere.backend.dto.common.PageResponse;
import com.CodeSphere.backend.model.*;
import com.CodeSphere.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        log.info("[ProblemService] Created problem: {} (ID: {})", problem.getTitle(), problem.getId());
        return mapToResponse(problem, true);
    }

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
