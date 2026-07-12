package com.CodeSphere.backend.service;

import com.CodeSphere.backend.config.StorageConfig;
import com.CodeSphere.backend.dto.file.FileUploadResponse;
import com.CodeSphere.backend.model.FileAttachment;
import com.CodeSphere.backend.model.StorageBackend;
import com.CodeSphere.backend.repository.FileAttachmentRepository;
import com.CodeSphere.backend.service.storage.StorageProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Unified file storage service.
 *
 * Provides a simple API for uploading, downloading, and managing files.
 * Delegates actual storage to the active {@link StorageProvider} backend.
 * Tracks metadata in the {@code file_attachments} table.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final StorageProvider storageProvider;
    private final FileAttachmentRepository fileAttachmentRepository;
    private final StorageConfig storageConfig;

    /**
     * Upload a file and associate it with an entity.
     *
     * @param file       the multipart file to upload
     * @param entityType the type of entity (PROBLEM, SUBMISSION, EDITORIAL, PROFILE)
     * @param entityId   the ID of the entity to associate with
     * @param uploaderId the user ID of the uploader
     * @return upload response with file metadata
     */
    @Transactional
    public FileUploadResponse upload(MultipartFile file, String entityType, Long entityId, Long uploaderId) {
        // Validate file
        validateFile(file);

        // Generate unique storage path
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
        String extension = getExtension(originalFilename);
        String storagePath = String.format("%s/%d/%s%s",
                entityType.toLowerCase(),
                entityId,
                UUID.randomUUID(),
                extension);

        try {
            // Compute checksum
            String checksum = computeChecksum(file);

            // Store the file
            String actualPath = storageProvider.store(file, storagePath);

            // Determine backend
            StorageBackend backend = "minio".equalsIgnoreCase(storageConfig.getActiveBackend())
                    ? StorageBackend.MINIO : StorageBackend.LOCAL;

            // Save metadata
            FileAttachment attachment = FileAttachment.builder()
                    .uploaderId(uploaderId)
                    .entityType(entityType.toUpperCase())
                    .entityId(entityId)
                    .fileName(originalFilename)
                    .filePath(actualPath)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .storageBackend(backend)
                    .checksum(checksum)
                    .build();

            attachment = fileAttachmentRepository.save(attachment);

            log.info("[FileStorage] Uploaded file: {} -> {} ({} bytes, {})",
                    originalFilename, actualPath, file.getSize(), backend);

            return FileUploadResponse.builder()
                    .fileId(attachment.getId())
                    .fileName(originalFilename)
                    .fileUrl(storageProvider.getUrl(actualPath))
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .checksum(checksum)
                    .storageBackend(backend.name())
                    .uploadedAt(attachment.getCreatedAt())
                    .build();

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    /**
     * Download a file by its attachment ID.
     *
     * @param fileId the file attachment ID
     * @return the file as a Resource for streaming
     */
    public Resource download(Long fileId) {
        FileAttachment attachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

        try {
            return storageProvider.load(attachment.getFilePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file: " + e.getMessage(), e);
        }
    }

    /**
     * Get file metadata by ID.
     */
    public FileAttachment getMetadata(Long fileId) {
        return fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));
    }

    /**
     * List all files associated with an entity.
     */
    public List<FileAttachment> listByEntity(String entityType, Long entityId) {
        return fileAttachmentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                entityType.toUpperCase(), entityId);
    }

    /**
     * Delete a file and its metadata.
     */
    @Transactional
    public void delete(Long fileId) {
        FileAttachment attachment = fileAttachmentRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("File not found with ID: " + fileId));

        try {
            storageProvider.delete(attachment.getFilePath());
        } catch (IOException e) {
            log.warn("[FileStorage] Failed to delete physical file: {}", attachment.getFilePath(), e);
        }

        fileAttachmentRepository.delete(attachment);
        log.info("[FileStorage] Deleted file: {} (ID: {})", attachment.getFileName(), fileId);
    }

    // ---- Private Helpers ----

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (file.getSize() > storageConfig.getMaxFileSize()) {
            throw new IllegalArgumentException(String.format(
                    "File size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    file.getSize(), storageConfig.getMaxFileSize()));
        }

        if (storageConfig.getAllowedContentTypes() != null &&
            !storageConfig.getAllowedContentTypes().isEmpty() &&
            !storageConfig.getAllowedContentTypes().contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "File type not allowed: " + file.getContentType());
        }
    }

    private String computeChecksum(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException e) {
            log.warn("[FileStorage] Failed to compute checksum", e);
            return null;
        }
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot >= 0 ? filename.substring(lastDot) : "";
    }
}
