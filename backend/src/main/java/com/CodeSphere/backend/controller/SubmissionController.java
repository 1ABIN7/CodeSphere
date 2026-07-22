package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.common.PageResponse;
import com.CodeSphere.backend.dto.submission.*;
import com.CodeSphere.backend.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for code submissions.
 *
 * Handles submitting code for judging and viewing submission results.
 * All endpoints require authentication.
 */
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Code submission and judging")
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    @Operation(summary = "Submit code for judging",
               description = "Submit source code against a problem. Returns verdict with AI analysis.")
    public ResponseEntity<SubmissionResponse> submitCode(
            @Valid @RequestBody SubmissionRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        SubmissionResponse response = submissionService.submitCode(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get submission result",
               description = "Get detailed submission result with per-test-case verdicts")
    public ResponseEntity<SubmissionResponse> getSubmission(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return ResponseEntity.ok(submissionService.getSubmission(id, userId));
    }

    @GetMapping("/{id}/analysis")
    @Operation(summary = "Get AI analysis",
               description = "Get detailed AI feedback for a submission")
    public ResponseEntity<JudgeResultResponse> getAnalysis(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return ResponseEntity.ok(submissionService.getAnalysis(id, userId));
    }

    @GetMapping("/problem/{problemId}")
    @Operation(summary = "My submissions for a problem",
               description = "List all your submissions for a specific problem")
    public ResponseEntity<PageResponse<SubmissionListResponse>> getMySubmissionsForProblem(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return ResponseEntity.ok(submissionService.getMySubmissionsForProblem(
                problemId, userId, page, size));
    }

    @GetMapping("/me")
    @Operation(summary = "My submission history",
               description = "List all your submissions across all problems")
    public ResponseEntity<PageResponse<SubmissionListResponse>> getMySubmissions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return ResponseEntity.ok(submissionService.getMySubmissions(userId, page, size));
    }

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.CodeSphere.backend.security.CustomUserDetails) {
            return ((com.CodeSphere.backend.security.CustomUserDetails) authentication.getPrincipal()).getId();
        }
        return null;
    }
}
