package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.interview.*;
import com.CodeSphere.backend.service.InterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/categories")
    public ResponseEntity<List<InterviewCategoryResponse>> getCategories() {
        return ResponseEntity.ok(interviewService.getCategories());
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionResultResponse> startSession(
            @RequestBody StartSessionRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(interviewService.startSession(userId, request));
    }

    @GetMapping("/sessions/{id}/next")
    public ResponseEntity<InterviewQuestionResponse> getNextQuestion(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        InterviewQuestionResponse nextQ = interviewService.getNextQuestion(id, userId);
        if (nextQ == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(nextQ);
    }

    @PostMapping("/sessions/{id}/answers")
    public ResponseEntity<AttemptFeedbackResponse> submitAnswer(
            @PathVariable Long id,
            @RequestParam Long questionId,
            @RequestBody SubmitAttemptRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(interviewService.submitAnswer(id, questionId, userId, request));
    }

    @PostMapping("/sessions/{id}/complete")
    public ResponseEntity<SessionResultResponse> completeSession(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(interviewService.completeSession(id, userId));
    }
    
    @GetMapping("/sessions/{id}/result")
    public ResponseEntity<SessionResultResponse> getSessionResult(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(interviewService.getSessionResult(id, userId));
    }

    @GetMapping("/performance")
    public ResponseEntity<PerformanceSummaryResponse> getPerformanceSummary(
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(interviewService.getPerformanceSummary(userId));
    }

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.CodeSphere.backend.security.CustomUserDetails) {
            return ((com.CodeSphere.backend.security.CustomUserDetails) authentication.getPrincipal()).getId();
        }
        return null;
    }
}
