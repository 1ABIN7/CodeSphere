package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.JudgeRequest;
import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.model.SubmissionStatus;
import com.CodeSphere.backend.messaging.SubmissionProducer;
import com.CodeSphere.backend.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
public class ProblemSubmissionController {

    private final SubmissionRepository submissionRepository;
    private final SubmissionProducer submissionProducer;

    @PostMapping("/{id}/submit")
    public ResponseEntity<Map<String, Object>> submitCode(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @RequestBody Map<String, String> requestBody) {
        
        String code = requestBody.get("code");
        String language = requestBody.get("language");
        
        // 1. Persist the submission as PENDING
        Submission submission = Submission.builder()
                .problemId(id)
                .userId(userId)
                .code(code)
                .language(language)
                .status(SubmissionStatus.PENDING)
                .build();
        Submission saved = submissionRepository.save(submission);
        
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
