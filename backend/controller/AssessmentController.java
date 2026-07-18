package com.codesphere.backend.controller;

import com.codesphere.backend.service.AssessmentService;
import com.codesphere.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AuditLogService auditLogService;

    /**
     * Retrieves specific details or structural metadata blueprints for a chosen assessment.
     */
    @GetMapping("/{assessmentId}")
    public ResponseEntity<?> getAssessmentDetails(@PathVariable Long assessmentId) {
        return ResponseEntity.ok(assessmentService.findById(assessmentId));
    }

    /**
     * Administrative resource management endpoint to create an assessment skeleton mapping.
     * Triggers a global audit logging trail record for administrative visibility.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createAssessmentBlueprint(@RequestBody Object assessmentDto, HttpServletRequest request) {
        Long adminId = getCurrentUserId();

        // Execute back-end structural database persistence
        Object created = assessmentService.createBlueprint(assessmentDto);

        // [Audit Log] Record critical structural configuration updates
        auditLogService.logAction(
                adminId,
                "ASSESSMENT-CREATE",
                "Created a new assessment configuration matrix blueprint",
                request
        );

        return ResponseEntity.ok(created);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        return (Long) authentication.getPrincipal();
    }
}