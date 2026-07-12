package com.CodeSphere.backend.dto.file;

import lombok.*;

import java.time.OffsetDateTime;

/**
 * Response DTO after a successful file upload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResponse {

    private Long fileId;
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String contentType;
    private String checksum;
    private String storageBackend;
    private OffsetDateTime uploadedAt;
}
