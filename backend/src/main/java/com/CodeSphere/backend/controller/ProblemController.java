package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.common.PageResponse;
import com.CodeSphere.backend.dto.problem.*;
import com.CodeSphere.backend.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for coding problem management.
 *
 * Public endpoints allow browsing published problems.
 * Admin/Examiner endpoints support full CRUD and test case management.
 */
@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Tag(name = "Problems", description = "Coding problem management and browsing")
public class ProblemController {

    private final ProblemService problemService;

    // ---- Public Endpoints ----

    @GetMapping
    @Operation(summary = "List published problems", description = "Browse problems with filtering, search, and pagination")
    public ResponseEntity<PageResponse<ProblemListResponse>> listProblems(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        return ResponseEntity.ok(problemService.listProblems(difficulty, search, sortBy, page, size, userId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get problem details", description = "Get a problem with sample test cases")
    public ResponseEntity<ProblemResponse> getProblem(
            @PathVariable Long id, Authentication authentication) {

        // If admin, show all test cases; otherwise only samples
        boolean isAdmin = isAdminOrExaminer(authentication);
        if (isAdmin) {
            return ResponseEntity.ok(problemService.getProblemByIdAdmin(id));
        }
        return ResponseEntity.ok(problemService.getProblemById(id));
    }

    @GetMapping("/{id}/test-cases")
    @Operation(summary = "Get test cases", description = "Admin sees all; candidates see only samples")
    public ResponseEntity<List<TestCaseResponse>> getTestCases(
            @PathVariable Long id, Authentication authentication) {

        boolean isAdmin = isAdminOrExaminer(authentication);
        return ResponseEntity.ok(problemService.getTestCases(id, isAdmin));
    }

    // ---- Admin/Examiner Endpoints ----

    @PostMapping
    @Operation(summary = "Create a new problem", description = "Admin/Examiner only")
    public ResponseEntity<ProblemResponse> createProblem(
            @Valid @RequestBody ProblemRequest request,
            Authentication authentication) {

        Long userId = getUserId(authentication);
        ProblemResponse response = problemService.createProblem(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a problem", description = "Admin/Examiner only")
    public ResponseEntity<ProblemResponse> updateProblem(
            @PathVariable Long id,
            @Valid @RequestBody ProblemRequest request) {
        return ResponseEntity.ok(problemService.updateProblem(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a problem", description = "Admin only")
    public ResponseEntity<Void> deleteProblem(@PathVariable Long id) {
        problemService.deleteProblem(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish or unpublish a problem")
    public ResponseEntity<ProblemResponse> togglePublish(
            @PathVariable Long id,
            @RequestParam boolean publish) {
        return ResponseEntity.ok(problemService.togglePublish(id, publish));
    }

    // ---- Test Case Management ----

    @PostMapping("/{id}/test-cases")
    @Operation(summary = "Add a test case to a problem")
    public ResponseEntity<TestCaseResponse> addTestCase(
            @PathVariable Long id,
            @Valid @RequestBody TestCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(problemService.addTestCase(id, request));
    }

    @PutMapping("/{problemId}/test-cases/{testCaseId}")
    @Operation(summary = "Update a test case")
    public ResponseEntity<TestCaseResponse> updateTestCase(
            @PathVariable Long problemId,
            @PathVariable Long testCaseId,
            @Valid @RequestBody TestCaseRequest request) {
        return ResponseEntity.ok(problemService.updateTestCase(problemId, testCaseId, request));
    }

    @DeleteMapping("/{problemId}/test-cases/{testCaseId}")
    @Operation(summary = "Delete a test case")
    public ResponseEntity<Void> deleteTestCase(
            @PathVariable Long problemId,
            @PathVariable Long testCaseId) {
        problemService.deleteTestCase(problemId, testCaseId);
        return ResponseEntity.noContent().build();
    }

    // ---- Helpers ----

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.CodeSphere.backend.security.CustomUserDetails) {
            return ((com.CodeSphere.backend.security.CustomUserDetails) authentication.getPrincipal()).getId();
        }
        return null;
    }

    private boolean isAdminOrExaminer(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") ||
                              a.getAuthority().equals("ROLE_ORG_ADMIN") ||
                              a.getAuthority().equals("ROLE_EXAMINER"));
    }
}
