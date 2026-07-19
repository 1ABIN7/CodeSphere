package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import com.CodeSphere.backend.service.AssessmentService;
import com.CodeSphere.backend.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;
    private final AuditLogService auditLogService;

    /**
     * Retrieves specific details or structural metadata blueprints for a chosen assessment.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Assessment> getAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssessmentById(id));
    }

    /**
     * Retrieves a list of all assessments.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<Assessment>> getAllAssessments() {
        return ResponseEntity.ok(assessmentService.getAllAssessments());
    }

    /**
     * Administrative resource management endpoint to create an assessment skeleton mapping.
     * Triggers a global audit logging trail record for administrative visibility.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Assessment> createAssessment(@RequestBody Assessment assessment, HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        Assessment created = assessmentService.createAssessment(assessment);

        auditLogService.logAction(
                adminId,
                "ASSESSMENT-CREATE",
                "Created a new assessment named: " + created.getTitle(),
                request
        );

        return ResponseEntity.ok(created);
    }

    /**
     * Updates an existing assessment profile.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Assessment> updateAssessment(@PathVariable Long id, @RequestBody Assessment assessment, HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        Assessment updated = assessmentService.updateAssessment(id, assessment);

        auditLogService.logAction(
                adminId,
                "ASSESSMENT-UPDATE",
                "Updated assessment record ID: " + id,
                request
        );

        return ResponseEntity.ok(updated);
    }

    /**
     * Deletes a target assessment context.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAssessment(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        assessmentService.deleteAssessment(id);

        auditLogService.logAction(
                adminId,
                "ASSESSMENT-DELETE",
                "Permanently removed assessment ID: " + id,
                request
        );

        return ResponseEntity.noContent().build();
    }

    /**
     * Clones an existing assessment configuration.
     */
    @PostMapping("/{id}/clone")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Assessment> cloneAssessment(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        Assessment cloned = assessmentService.cloneAssessment(id);

        auditLogService.logAction(
                adminId,
                "ASSESSMENT-CLONE",
                "Cloned assessment ID: " + id + " into new instance ID: " + cloned.getId(),
                request
        );

        return ResponseEntity.ok(cloned);
    }

    /**
     * Publishes an assessment making it visible to candidates.
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Assessment> publishAssessment(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        Assessment published = assessmentService.publishAssessment(id);

        auditLogService.logAction(
                adminId,
                "ASSESSMENT-PUBLISH",
                "Published assessment ID: " + id,
                request
        );

        return ResponseEntity.ok(published);
    }

    /**
     * Retracts an active assessment from circulation.
     */
    @PutMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Assessment> unpublishAssessment(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        Assessment unpublished = assessmentService.unpublishAssessment(id);

        auditLogService.logAction(
                adminId,
                "ASSESSMENT-UNPUBLISH",
                "Unpublished active assessment ID: " + id,
                request
        );

        return ResponseEntity.ok(unpublished);
    }

    /**
     * Assigns a specific assessment to a target candidate profile with a processing deadline.
     */
    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<AssessmentAssignment> assignAssessment(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
            HttpServletRequest request) {

        Long issuerId = getCurrentUserId();
        AssessmentAssignment assignment = assessmentService.assignAssessment(id, userId, deadline);

        auditLogService.logAction(
                issuerId,
                "ASSESSMENT-ASSIGN",
                "Assigned assessment ID: " + id + " to candidate user ID: " + userId,
                request
        );

        return ResponseEntity.ok(assignment);
    }

    /**
     * Fetches candidates assigned to the given assessment configuration.
     */
    @GetMapping("/{id}/assigned-candidates")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<List<AssessmentAssignment>> getAssignedCandidates(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssignedCandidates(id));
    }

    /**
     * Safety utilities to extract principal details context out of Spring Security context.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        return (Long) authentication.getPrincipal();
    }
}