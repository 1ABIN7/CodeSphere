package com.CodeSphere.backend.service.storage;

import com.CodeSphere.backend.config.StorageConfig;
import io.minio.*;
import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * MinIO S3-compatible storage provider.
 *
 * Uses the MinIO Java SDK to store, retrieve, and delete objects
 * in an S3-compatible bucket. Activated when storage.active-backend=minio.
 */
@Component
@ConditionalOnProperty(name = "storage.active-backend", havingValue = "minio")
@Slf4j
public class MinioStorageProvider implements StorageProvider {

    private final MinioClient minioClient;
    private final String bucket;

    public MinioStorageProvider(StorageConfig config) {
        StorageConfig.Minio minioConfig = config.getMinio();

        this.minioClient = MinioClient.builder()
                .endpoint(minioConfig.getEndpoint())
                .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                .build();

        this.bucket = minioConfig.getBucket();

        // Ensure bucket exists
        try {
            boolean bucketExists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!bucketExists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("[MinIO] Created bucket: {}", bucket);
            }
            log.info("[MinIO] Initialized with bucket: {}", bucket);
        } catch (Exception e) {
            log.warn("[MinIO] Could not verify/create bucket: {}. MinIO may not be available.", e.getMessage());
        }
    }

    @Override
    public String store(MultipartFile file, String storagePath) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());

            log.debug("[MinIO] Stored object: {}/{} ({} bytes)", bucket, storagePath, file.getSize());
            return storagePath;

        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException |
                 ServerException | XmlParserException e) {
            throw new IOException("Failed to store file in MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource load(String storagePath) throws IOException {
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());

            return new InputStreamResource(stream);

        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException |
                 ServerException | XmlParserException e) {
            throw new IOException("Failed to load file from MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storagePath) throws IOException {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());

            log.debug("[MinIO] Deleted object: {}/{}", bucket, storagePath);

        } catch (ErrorResponseException | InsufficientDataException | InternalException |
                 InvalidKeyException | InvalidResponseException | NoSuchAlgorithmException |
                 ServerException | XmlParserException e) {
            throw new IOException("Failed to delete file from MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(storagePath)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getUrl(String storagePath) {
        return String.format("/api/files/download/%s", storagePath);
    }
}
