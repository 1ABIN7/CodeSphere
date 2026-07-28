package com.CodeSphere.backend.messaging;

import com.CodeSphere.backend.config.RabbitMQConfig;
import com.CodeSphere.backend.dto.DockerExecutionResult;
import com.CodeSphere.backend.dto.JudgeRequest;
import com.CodeSphere.backend.model.Problem;
import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.model.SubmissionStatus;
import com.CodeSphere.backend.repository.ProblemRepository;
import com.CodeSphere.backend.repository.SubmissionRepository;
import com.CodeSphere.backend.service.CodeAnalysisService;
import com.CodeSphere.backend.service.DockerExecutionService;
import com.CodeSphere.backend.service.SkillScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ consumer for the judge queue.
 *
 * Processes two types of messages:
 * 1. Full submissions — compile, run all test cases, update DB, push WebSocket result
 * 2. Run-only requests — run code without persisting DB state, push WebSocket result
 *
 * After judging, runs CodeAnalysis (complexity, quality, plagiarism hints) and
 * updates the user's skill scores on ACCEPTED verdicts.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionConsumer {

    private final DockerExecutionService dockerExecutionService;
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final CodeAnalysisService codeAnalysisService;
    private final SkillScoreService skillScoreService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_JUDGE)
    public void consumeMessage(JudgeRequest request) {
        log.info("Received judge request for submission ID: {}, runOnly: {}",
                request.getSubmissionId(), request.isRunOnly());

        if (request.isRunOnly()) {
            // Process run-code request (no DB persistence of final state)
            DockerExecutionResult result = dockerExecutionService.runCode(
                    request.getCode(),
                    request.getLanguage(),
                    request.getProblemId()
            );
            messagingTemplate.convertAndSend(
                    "/queue/submission-result/" + request.getSubmissionId(),
                    buildWebSocketPayload(request.getSubmissionId(), result, null)
            );
            log.info("Finished processing run request for ID: {}", request.getSubmissionId());
            return;
        }

        // 1. Load submission
        Submission submission = submissionRepository.findById(request.getSubmissionId()).orElse(null);
        if (submission == null) {
            log.error("Submission with ID {} not found in database", request.getSubmissionId());
            return;
        }

        // 2. Mark as RUNNING
        submission.setStatus(SubmissionStatus.RUNNING);
        submissionRepository.save(submission);

        // 3. Execute code via Docker (or mock)
        DockerExecutionResult result;
        try {
            result = dockerExecutionService.executeSubmission(
                    submission.getId(),
                    submission.getCode(),
                    submission.getLanguage(),
                    submission.getProblemId()
            );
        } catch (Exception e) {
            log.error("Docker execution failed for submission {}: {}", submission.getId(), e.getMessage(), e);
            submission.setStatus(SubmissionStatus.RUNTIME_ERROR);
            submission.setErrorMessage("Internal judge error: " + e.getMessage());
            submissionRepository.save(submission);
            return;
        }

        // 4. Parse verdict safely — fall back to RUNTIME_ERROR for unknown values
        SubmissionStatus finalStatus;
        try {
            finalStatus = SubmissionStatus.valueOf(result.getVerdict());
        } catch (IllegalArgumentException | NullPointerException ex) {
            log.warn("Unknown verdict '{}' for submission {}, defaulting to RUNTIME_ERROR",
                    result.getVerdict(), submission.getId());
            finalStatus = SubmissionStatus.RUNTIME_ERROR;
        }

        // 5. Run code analysis (static: complexity, quality, plagiarism hints)
        Map<String, Object> analysisResult = new HashMap<>();
        Map<String, Object> aiFeedback = new HashMap<>();
        try {
            int passed = submission.getTestCasesPassed() != null ? submission.getTestCasesPassed() : 0;
            int total  = submission.getTotalTestCases()  != null ? submission.getTotalTestCases()  : 0;
            var codeAnalysis = codeAnalysisService.analyze(
                    submission.getCode(),
                    submission.getLanguage(),
                    finalStatus,
                    passed,
                    total
            );
            // Build complexity map
            analysisResult.put("timeComplexity",  codeAnalysis.getEstimatedTimeComplexity());
            analysisResult.put("spaceComplexity", codeAnalysis.getEstimatedSpaceComplexity());
            analysisResult.put("rawMetrics",      codeAnalysis.getRawMetrics());
            // Build AI feedback map
            aiFeedback.put("qualityScore",           codeAnalysis.getCodeQualityScore());
            aiFeedback.put("qualityIssues",          codeAnalysis.getCodeQualityIssues());
            aiFeedback.put("antiPatterns",           codeAnalysis.getAntiPatterns());
            aiFeedback.put("optimizationSuggestions",codeAnalysis.getOptimizationSuggestions());
            aiFeedback.put("performanceHints",       codeAnalysis.getPerformanceHints());
            aiFeedback.put("similarityScore",        codeAnalysis.getSimilarityScore());
            aiFeedback.put("potentialPlagiarism",    codeAnalysis.getPotentialPlagiarism());
        } catch (Exception e) {
            log.warn("Code analysis failed for submission {}: {}", submission.getId(), e.getMessage());
        }

        // 6. Persist final state
        submission.setStatus(finalStatus);
        if (result.getExecTime() != null)   submission.setExecTime(result.getExecTime());
        if (result.getExecMemory() != null) submission.setExecMemory(result.getExecMemory());
        if (result.getErrorMessage() != null) submission.setErrorMessage(result.getErrorMessage());
        submission.setComplexityAnalysis(analysisResult);
        submission.setAiFeedback(aiFeedback);
        submissionRepository.save(submission);

        // 7. Update skill scores on ACCEPTED
        if (finalStatus == SubmissionStatus.ACCEPTED && submission.getUserId() != null) {
            try {
                problemRepository.findById(submission.getProblemId()).ifPresent(problem ->
                        skillScoreService.updateScoresOnAccepted(submission.getUserId(), problem)
                );
            } catch (Exception e) {
                log.warn("Skill score update failed for user {}: {}", submission.getUserId(), e.getMessage());
            }
        }

        // 8. Push WebSocket result
        messagingTemplate.convertAndSend(
                "/queue/submission-result/" + submission.getId(),
                buildWebSocketPayload(submission.getId(), result, finalStatus)
        );
        log.info("Finished judging submission ID: {} with verdict: {}", submission.getId(), finalStatus);
    }

    // ---- Helpers ----

    private Map<String, Object> buildWebSocketPayload(Long submissionId, DockerExecutionResult result,
                                                       SubmissionStatus status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("submissionId", submissionId);
        payload.put("verdict", result.getVerdict());
        payload.put("execTime", result.getExecTime());
        payload.put("execMemory", result.getExecMemory());
        payload.put("errorMessage", result.getErrorMessage());
        if (status != null) payload.put("status", status.name());
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> safeToMap(Object obj) {
        if (obj instanceof Map) return (Map<String, Object>) obj;
        return new HashMap<>();
    }
}
