package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.FileAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for FileAttachment entity operations.
 */
@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    List<FileAttachment> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    List<FileAttachment> findByUploaderIdOrderByCreatedAtDesc(Long uploaderId);

    long countByEntityTypeAndEntityId(String entityType, Long entityId);

    void deleteByEntityTypeAndEntityId(String entityType, Long entityId);
}
