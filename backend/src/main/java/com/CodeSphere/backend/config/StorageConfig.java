package com.CodeSphere.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for file storage backends.
 * Supports local filesystem and MinIO S3-compatible storage.
 *
 * Configure via application.yml under the {@code storage} prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class StorageConfig {

    /**
     * Active backend: "local" or "minio".
     */
    private String activeBackend = "local";

    /**
     * Maximum file upload size in bytes (default: 50MB).
     */
    private long maxFileSize = 52428800L;

    /**
     * Allowed MIME types for uploads.
     */
    private List<String> allowedContentTypes = List.of(
            "text/plain",
            "text/csv",
            "application/pdf",
            "application/json",
            "application/zip",
            "application/x-tar",
            "application/gzip",
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/svg+xml"
    );

    /**
     * Local storage configuration.
     */
    private Local local = new Local();

    /**
     * MinIO storage configuration.
     */
    private Minio minio = new Minio();

    @Getter
    @Setter
    public static class Local {
        /**
         * Root directory for local file storage.
         */
        private String rootPath = "./storage/uploads";
    }

    @Getter
    @Setter
    public static class Minio {
        private String endpoint = "http://localhost:9000";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private String bucket = "codesphere-assets";
    }
}
