package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.service.AuditLogService;
import com.CodeSphere.backend.service.StorageService; // Assumes your file storage logic lives here
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;
    private final AuditLogService auditLogService;

    /**
     * Uploads a multipart file, persists it via StorageService, and logs the action to the audit trail.
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        HttpServletRequest request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot upload an empty file.");
        }

        Long userId = getCurrentUserId();

        // Save the file (to Local Filesystem, MinIO, S3, etc.)
        String storedFileName = storageService.store(file);

        // [Audit Log] Log the critical File Upload action
        auditLogService.logAction(
                userId,
                "FILE-UPLOAD",
                String.format("File: %s | Saved Name: %s | Size: %d bytes",
                        file.getOriginalFilename(), storedFileName, file.getSize()),
                request
        );

        // Return details of the uploaded file
        return ResponseEntity.ok(Map.of(
                "message", "File uploaded successfully",
                "fileName", storedFileName
        ));
    }

    /**
     * Extracts the authenticated user's ID from the security context.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        return (Long) authentication.getPrincipal();
    }
}