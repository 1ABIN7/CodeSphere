package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Tracks uploaded file metadata for any entity in the system.
 *
 * Uses a polymorphic pattern (entity_type + entity_id) to link files
 * to Problems, Submissions, Editorials, or Profiles without requiring
 * separate join tables for each.
 */
@Entity
@Table(name = "file_attachments", indexes = {
    @Index(name = "idx_files_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_files_uploader", columnList = "uploader_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uploader_id")
    private Long uploaderId;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType; // PROBLEM, SUBMISSION, EDITORIAL, PROFILE

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // bytes

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_backend", nullable = false, length = 20)
    @Builder.Default
    private StorageBackend storageBackend = StorageBackend.LOCAL;

    @Column(length = 128)
    private String checksum; // SHA-256

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}
