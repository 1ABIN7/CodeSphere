package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.file.FileUploadResponse;
import com.CodeSphere.backend.model.FileAttachment;
import com.CodeSphere.backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for file storage operations.
 *
 * Supports uploading, downloading, and managing file attachments
 * associated with problems, submissions, editorials, and profiles.
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload, download, and management")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file",
               description = "Upload a file and associate it with an entity (PROBLEM, SUBMISSION, EDITORIAL, PROFILE)")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entityType") String entityType,
            @RequestParam("entityId") Long entityId,
            Authentication authentication) {

        Long uploaderId = getUserId(authentication);
        FileUploadResponse response = fileStorageService.upload(file, entityType, entityId, uploaderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Download a file", description = "Download a file by its attachment ID")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        FileAttachment metadata = fileStorageService.getMetadata(id);
        Resource resource = fileStorageService.download(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getFileName() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/metadata")
    @Operation(summary = "Get file metadata", description = "Get metadata about an uploaded file")
    public ResponseEntity<FileUploadResponse> getFileMetadata(@PathVariable Long id) {
        FileAttachment metadata = fileStorageService.getMetadata(id);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileId(metadata.getId())
                .fileName(metadata.getFileName())
                .fileUrl("/api/files/" + metadata.getId())
                .fileSize(metadata.getFileSize())
                .contentType(metadata.getContentType())
                .checksum(metadata.getChecksum())
                .storageBackend(metadata.getStorageBackend().name())
                .uploadedAt(metadata.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file", description = "Delete a file and its metadata")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id) {
        fileStorageService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "List files for an entity",
               description = "List all file attachments for a specific entity")
    public ResponseEntity<List<FileUploadResponse>> listFilesByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId) {

        List<FileAttachment> files = fileStorageService.listByEntity(entityType, entityId);

        List<FileUploadResponse> response = files.stream()
                .map(f -> FileUploadResponse.builder()
                        .fileId(f.getId())
                        .fileName(f.getFileName())
                        .fileUrl("/api/files/" + f.getId())
                        .fileSize(f.getFileSize())
                        .contentType(f.getContentType())
                        .checksum(f.getChecksum())
                        .storageBackend(f.getStorageBackend().name())
                        .uploadedAt(f.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof com.CodeSphere.backend.security.CustomUserDetails) {
            return ((com.CodeSphere.backend.security.CustomUserDetails) authentication.getPrincipal()).getId();
        }
        return null;
    }
}
