package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.ProctoringEventRequest;
import com.CodeSphere.backend.model.ProctoringConfig;
import com.CodeSphere.backend.model.ProctoringEvent;
import com.CodeSphere.backend.service.ProctoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/proctoring")
@RequiredArgsConstructor
public class ProctoringController {

    private final ProctoringService proctoringService;

    @PostMapping("/sessions/{sessionId}/events")
    public ResponseEntity<ProctoringEvent> recordEvent(
            @PathVariable Long sessionId,
            @RequestBody ProctoringEventRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(proctoringService.recordEvent(sessionId, userId, request));
    }

    @PostMapping("/sessions/{sessionId}/snapshot")
    public ResponseEntity<ProctoringEvent> uploadSnapshot(
            @PathVariable Long sessionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "WEBCAM_SNAPSHOT") String eventType,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return ResponseEntity.ok(proctoringService.uploadSnapshot(sessionId, userId, file, eventType));
    }

    @GetMapping("/sessions/{sessionId}/events")
    public ResponseEntity<List<ProctoringEvent>> getSessionEvents(@PathVariable Long sessionId) {
        // In reality, this should be restricted to EXAMINER/ADMIN roles
        return ResponseEntity.ok(proctoringService.getSessionEvents(sessionId));
    }

    @GetMapping("/assessments/{assessmentId}/config")
    public ResponseEntity<ProctoringConfig> getConfig(@PathVariable Long assessmentId) {
        return ResponseEntity.ok(proctoringService.getConfig(assessmentId));
    }

    @PutMapping("/assessments/{assessmentId}/config")
    public ResponseEntity<ProctoringConfig> updateConfig(
            @PathVariable Long assessmentId,
            @RequestBody ProctoringConfig config) {
        // Restricted to EXAMINER/ADMIN roles
        return ResponseEntity.ok(proctoringService.updateConfig(assessmentId, config));
    }

    @PutMapping("/sessions/{sessionId}/flag")
    public ResponseEntity<Void> flagSession(
            @PathVariable Long sessionId,
            @RequestParam String reason) {
        // Restricted to EXAMINER/ADMIN roles
        proctoringService.flagSession(sessionId, reason);
        return ResponseEntity.ok().build();
    }

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.CodeSphere.backend.security.CustomUserDetails) {
            return ((com.CodeSphere.backend.security.CustomUserDetails) authentication.getPrincipal()).getId();
        }
        return null;
    }
}
