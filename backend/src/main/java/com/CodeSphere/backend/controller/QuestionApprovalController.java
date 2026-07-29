package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.service.QuestionApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Optional: delete if not using method-level security
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/questions/{id}")
@CrossOrigin(origins = "*")
public class QuestionApprovalController {

    private final QuestionApprovalService approvalService;

    public QuestionApprovalController(QuestionApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    // SUBMIT: POST /api/v1/questions/{id}/submit-for-approval
    @PostMapping("/submit-for-approval")
    public ResponseEntity<Question> submitForApproval(@PathVariable Long id) {
        Question question = approvalService.submitForApproval(id);
        return ResponseEntity.ok(question);
    }

    // APPROVE: POST /api/v1/questions/{id}/approve
    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Question> approveQuestion(@PathVariable Long id) {
        Question question = approvalService.approveQuestion(id);
        return ResponseEntity.ok(question);
    }

    // REJECT: POST /api/v1/questions/{id}/reject
    @PostMapping("/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER', 'INSTRUCTOR')")
    public ResponseEntity<Question> rejectQuestion(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String feedback = body.getOrDefault("feedback", "No feedback provided.");
        Question question = approvalService.rejectQuestion(id, feedback);
        return ResponseEntity.ok(question);
    }
}
