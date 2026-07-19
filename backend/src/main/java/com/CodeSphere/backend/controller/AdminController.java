package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.service.AuditLogService;
import com.CodeSphere.backend.service.UserService;
import com.CodeSphere.backend.service.ScoreService; // Assumes your score updating logic lives here
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ScoreService scoreService;
    private final AuditLogService auditLogService;

    /**
     * Manually overrides a submission score and logs the action for security oversight.
     */
    @PutMapping("/submissions/{submissionId}/score")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateScore(@PathVariable Long submissionId,
                                            @RequestParam Double newScore,
                                            HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        scoreService.updateSubmissionScore(submissionId, newScore);

        // [Audit Log] Log Score Change
        auditLogService.logAction(
            adminId,
            "SCORE-CHANGE",
            String.format("Submission ID: %d | New Score: %.2f", submissionId, newScore),
            request
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Updates privileges/roles for a target system user and captures the event in the audit index.
     */
    @PutMapping("/users/{targetUserId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeUserRole(@PathVariable Long targetUserId,
                                               @RequestParam String newRole,
                                               HttpServletRequest request) {
        Long adminId = getCurrentUserId();
        userService.updateUserRole(targetUserId, newRole);

        // [Audit Log] Log Role Modification
        auditLogService.logAction(
            adminId,
            "USER-ROLE-CHANGE",
            String.format("User ID: %d | Role Assigned: %s", targetUserId, newRole),
            request
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Extract the administrative user ID out from the current security framework thread.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        return (Long) authentication.getPrincipal();
    }
}