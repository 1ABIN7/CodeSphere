package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.JudgeRequest;
import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.model.SubmissionStatus;
import com.CodeSphere.backend.messaging.SubmissionProducer;
import com.CodeSphere.backend.repository.SubmissionRepository;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemSubmissionController {

    private final SubmissionRepository submissionRepository;
    private final SubmissionProducer submissionProducer;
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;

    @PostMapping("/{id}/submit")
    @Transactional
    public ResponseEntity<Map<String, Object>> submitCode(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long assessmentSessionId,
            @RequestParam(required = false) Long assessmentQuestionId,
            Authentication authentication,
            @RequestBody Map<String, String> requestBody) {
        
        String code = requestBody.get("code");
        String language = requestBody.get("language");
        
        // 1. Persist the submission as PENDING
        Long authenticatedUserId = authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user ? user.getId() : userId;
        if (assessmentSessionId != null || assessmentQuestionId != null) {
            if (assessmentSessionId == null || assessmentQuestionId == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Both assessment session and question are required."));
            }
            var session = assessmentSessionRepository.findById(assessmentSessionId).orElse(null);
            if (session == null || authenticatedUserId == null || !authenticatedUserId.equals(session.getCandidate().getId())) {
                return ResponseEntity.status(403).body(Map.of("message", "This assessment session is not available."));
            }
            boolean belongsToSession = session.getQuestionIdsSnapshot().contains(assessmentQuestionId);
            if (!belongsToSession) return ResponseEntity.badRequest().body(Map.of("message", "Question does not belong to this assessment session."));
        }

        Submission submission = Submission.builder()
                .problemId(id)
                .userId(authenticatedUserId)
                .code(code)
                .language(language)
                .status(SubmissionStatus.PENDING)
                .build();
        Submission saved = submissionRepository.save(submission);
        if (assessmentSessionId != null) {
            assessmentAnswerRepository.findBySessionIdAndQuestionId(assessmentSessionId, assessmentQuestionId).ifPresent(answer -> {
                answer.setCodingSubmissionId(saved.getId());
                answer.setEvaluationStatus("JUDGING");
                assessmentAnswerRepository.save(answer);
            });
        }
        
        // 2. Queue the judging task
        JudgeRequest judgeRequest = JudgeRequest.builder()
                .submissionId(saved.getId())
                .problemId(id)
                .code(code)
                .language(language)
                .isRunOnly(false)
                .build();
        submissionProducer.pushToQueue(judgeRequest);
        
        return ResponseEntity.ok(Map.of(
                "submissionId", saved.getId(),
                "status", saved.getStatus()
        ));
    }

    @PostMapping("/{id}/run")
    public ResponseEntity<Map<String, Object>> runCode(
            @PathVariable Long id,
            @RequestBody Map<String, String> requestBody) {
        
        String code = requestBody.get("code");
        String language = requestBody.get("language");
        
        // Generate a temporary negative ID for WebSocket subscription
        long tempId = -ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        
        // Queue the run task
        JudgeRequest judgeRequest = JudgeRequest.builder()
                .submissionId(tempId)
                .problemId(id)
                .code(code)
                .language(language)
                .isRunOnly(true)
                .build();
        submissionProducer.pushToQueue(judgeRequest);
        
        return ResponseEntity.ok(Map.of(
                "runId", tempId,
                "status", "RUNNING"
        ));
    }
}
