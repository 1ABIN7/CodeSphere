package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.common.PageResponse;
import com.CodeSphere.backend.dto.submission.*;
import com.CodeSphere.backend.model.*;
import com.CodeSphere.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the code submission workflow.
 *
 * Flow:
 * 1. Validate submission (problem exists, language supported)
 * 2. Save submission with PENDING status
 * 3. Delegate to JudgeEngineService for sandboxed execution
 * 4. Delegate to CodeAnalysisService for AI feedback
 * 5. Persist per-test-case results
 * 6. Update problem statistics
 * 7. Update user skill scores
 * 8. Return comprehensive result
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final SubmissionResultRepository submissionResultRepository;
    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;
    private final JudgeEngineService judgeEngineService;
    private final CodeAnalysisService codeAnalysisService;
    private final SkillScoreService skillScoreService;

    /**
     * Submit code for judging against a problem.
     */
    @Transactional
    public SubmissionResponse submitCode(SubmissionRequest request, Long userId) {
        // 1. Validate problem exists
        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Problem not found with ID: " + request.getProblemId()));

        if (!problem.getIsPublished()) {
            throw new IllegalStateException("Cannot submit to an unpublished problem");
        }

        // 2. Validate language
        String language = request.getLanguage().toLowerCase();
        List<String> supported = List.of("java", "python", "cpp", "c", "javascript");
        if (!supported.contains(language)) {
            throw new IllegalArgumentException("Unsupported language: " + language +
                    ". Supported: " + String.join(", ", supported));
        }

        // 3. Get all test cases
        List<TestCase> testCases = testCaseRepository.findByProblemIdOrderByOrderIndexAsc(
                problem.getId());
        if (testCases.isEmpty()) {
            throw new IllegalStateException("Problem has no test cases configured");
        }

        // 4. Create submission with RUNNING status
        Submission submission = Submission.builder()
                .problemId(problem.getId())
                .userId(userId)
                .code(request.getCode())
                .language(language)
                .status(SubmissionStatus.RUNNING)
                .totalTestCases(testCases.size())
                .build();
        submission = submissionRepository.save(submission);

        // 5. Run through judge engine
        JudgeEngineService.JudgeResult judgeResult = judgeEngineService.judge(
                request.getCode(), language, testCases,
                problem.getTimeLimit(), problem.getMemoryLimit());

        // 6. Run AI analysis
        JudgeResultResponse analysis = codeAnalysisService.analyze(
                request.getCode(), language,
                judgeResult.overallStatus(),
                judgeResult.testCasesPassed(),
                judgeResult.totalTestCases());

        // 7. Update submission with results
        submission.setStatus(judgeResult.overallStatus());
        submission.setScore(judgeResult.score());
        submission.setTestCasesPassed(judgeResult.testCasesPassed());
        submission.setTotalTestCases(judgeResult.totalTestCases());
        submission.setExecTime(judgeResult.maxExecTime());
        submission.setExecMemory(judgeResult.maxExecMemory());
        submission.setErrorMessage(judgeResult.compilationError());

        // Store AI analysis as JSONB
        Map<String, Object> aiFeedback = new LinkedHashMap<>();
        aiFeedback.put("codeQualityScore", analysis.getCodeQualityScore());
        aiFeedback.put("codeQualityIssues", analysis.getCodeQualityIssues());
        aiFeedback.put("antiPatterns", analysis.getAntiPatterns());
        aiFeedback.put("optimizationSuggestions", analysis.getOptimizationSuggestions());
        aiFeedback.put("performanceHints", analysis.getPerformanceHints());
        submission.setAiFeedback(aiFeedback);

        Map<String, Object> complexityData = new LinkedHashMap<>();
        complexityData.put("estimatedTimeComplexity", analysis.getEstimatedTimeComplexity());
        complexityData.put("estimatedSpaceComplexity", analysis.getEstimatedSpaceComplexity());
        complexityData.put("rawMetrics", analysis.getRawMetrics());
        submission.setComplexityAnalysis(complexityData);

        submission = submissionRepository.save(submission);

        // 8. Save per-test-case results
        List<SubmissionResult> results = new ArrayList<>();
        for (JudgeEngineService.TestCaseExecResult tcResult : judgeResult.testCaseResults()) {
            SubmissionResult sr = SubmissionResult.builder()
                    .submission(submission)
                    .testCaseId(tcResult.testCaseId())
                    .status(tcResult.status())
                    .actualOutput(tcResult.actualOutput())
                    .execTime(tcResult.execTime())
                    .execMemory(tcResult.execMemory())
                    .errorOutput(tcResult.errorOutput())
                    .build();
            results.add(submissionResultRepository.save(sr));
        }

        // 9. Update problem statistics
        boolean isAccepted = judgeResult.overallStatus() == SubmissionStatus.ACCEPTED;
        problem.incrementSubmissions(isAccepted);
        problemRepository.save(problem);

        // 10. Update skill scores if accepted
        if (isAccepted && userId != null) {
            skillScoreService.updateScoresOnAccepted(userId, problem);
        }

        log.info("[SubmissionService] Submission {} for problem {}: {} ({}/{} passed, score={})",
                submission.getId(), problem.getId(), judgeResult.overallStatus(),
                judgeResult.testCasesPassed(), judgeResult.totalTestCases(), judgeResult.score());

        return mapToResponse(submission, problem, results, testCases, analysis);
    }

    /**
     * Get a submission by ID.
     */
    public SubmissionResponse getSubmission(Long id, Long userId) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Submission not found with ID: " + id));

        // Users can only view their own submissions (unless admin)
        if (!submission.getUserId().equals(userId)) {
            throw new SecurityException("You can only view your own submissions");
        }

        Problem problem = problemRepository.findById(submission.getProblemId())
                .orElseThrow(() -> new NoSuchElementException("Problem not found"));

        List<SubmissionResult> results = submissionResultRepository
                .findBySubmissionIdOrderByTestCaseIdAsc(submission.getId());
        List<TestCase> testCases = testCaseRepository
                .findByProblemIdOrderByOrderIndexAsc(problem.getId());

        return mapToResponse(submission, problem, results, testCases, null);
    }

    /**
     * Get AI analysis for a submission.
     */
    public JudgeResultResponse getAnalysis(Long submissionId, Long userId) {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission not found with ID: " + submissionId));

        if (!submission.getUserId().equals(userId)) {
            throw new SecurityException("You can only view your own submissions");
        }

        // Re-run analysis from stored data
        return codeAnalysisService.analyze(
                submission.getCode(), submission.getLanguage(),
                submission.getStatus(),
                submission.getTestCasesPassed(),
                submission.getTotalTestCases());
    }

    /**
     * List submissions for a specific problem by the current user.
     */
    public PageResponse<SubmissionListResponse> getMySubmissionsForProblem(
            Long problemId, Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Submission> submissions = submissionRepository
                .findByProblemIdAndUserIdOrderByCreatedAtDesc(problemId, userId, pageable);

        Problem problem = problemRepository.findById(problemId).orElse(null);
        String problemTitle = problem != null ? problem.getTitle() : "Unknown";

        Page<SubmissionListResponse> responsePage = submissions.map(s ->
                mapToListResponse(s, problemTitle));

        return PageResponse.of(responsePage);
    }

    /**
     * List all submissions by the current user.
     */
    public PageResponse<SubmissionListResponse> getMySubmissions(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Submission> submissions = submissionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        Page<SubmissionListResponse> responsePage = submissions.map(s -> {
            Problem p = problemRepository.findById(s.getProblemId()).orElse(null);
            String title = p != null ? p.getTitle() : "Unknown";
            return mapToListResponse(s, title);
        });

        return PageResponse.of(responsePage);
    }

    // ---- Mapping ----

    private SubmissionResponse mapToResponse(Submission s, Problem problem,
                                              List<SubmissionResult> results,
                                              List<TestCase> testCases,
                                              JudgeResultResponse analysis) {
        // Build test case result DTOs
        Map<Long, TestCase> tcMap = testCases.stream()
                .collect(Collectors.toMap(TestCase::getId, tc -> tc));

        List<SubmissionResponse.TestCaseResultResponse> tcResponses = results.stream()
                .map(r -> {
                    TestCase tc = tcMap.get(r.getTestCaseId());
                    boolean isSample = tc != null && Boolean.TRUE.equals(tc.getIsSample());

                    return SubmissionResponse.TestCaseResultResponse.builder()
                            .testCaseId(r.getTestCaseId())
                            .status(r.getStatus().name())
                            .actualOutput(isSample ? r.getActualOutput() : null)
                            .expectedOutput(isSample && tc != null ? tc.getExpectedOutput() : null)
                            .input(isSample && tc != null ? tc.getInputData() : null)
                            .execTime(r.getExecTime())
                            .execMemory(r.getExecMemory())
                            .errorOutput(isSample ? r.getErrorOutput() : null)
                            .isSample(isSample)
                            .build();
                })
                .collect(Collectors.toList());

        return SubmissionResponse.builder()
                .id(s.getId())
                .problemId(s.getProblemId())
                .problemTitle(problem.getTitle())
                .language(s.getLanguage())
                .code(s.getCode())
                .status(s.getStatus().name())
                .score(s.getScore())
                .execTime(s.getExecTime())
                .execMemory(s.getExecMemory())
                .errorMessage(s.getErrorMessage())
                .testCasesPassed(s.getTestCasesPassed())
                .totalTestCases(s.getTotalTestCases())
                .testCaseResults(tcResponses)
                .aiFeedback(s.getAiFeedback())
                .complexityAnalysis(s.getComplexityAnalysis())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private SubmissionListResponse mapToListResponse(Submission s, String problemTitle) {
        return SubmissionListResponse.builder()
                .id(s.getId())
                .problemId(s.getProblemId())
                .problemTitle(problemTitle)
                .language(s.getLanguage())
                .status(s.getStatus().name())
                .score(s.getScore())
                .execTime(s.getExecTime())
                .execMemory(s.getExecMemory())
                .testCasesPassed(s.getTestCasesPassed())
                .totalTestCases(s.getTotalTestCases())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
