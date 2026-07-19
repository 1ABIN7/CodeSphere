package com.CodeSphere.backend.service.storage;

import com.CodeSphere.backend.config.StorageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Local filesystem storage provider.
 *
 * Stores files under a configurable root directory. Used as the default
 * storage backend for development environments.
 */
@Component
@ConditionalOnProperty(name = "storage.active-backend", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageProvider implements StorageProvider {

    private final Path rootPath;

    public LocalStorageProvider(StorageConfig config) {
        this.rootPath = Paths.get(config.getLocal().getRootPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
            log.info("[LocalStorage] Initialized at: {}", this.rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create local storage directory: " + this.rootPath, e);
        }
    }

    @Override
    public String store(MultipartFile file, String storagePath) throws IOException {
        Path targetPath = rootPath.resolve(storagePath).normalize();

        // Security: prevent directory traversal
        if (!targetPath.startsWith(rootPath)) {
            throw new SecurityException("Cannot store file outside root directory");
        }

        // Create parent directories
        Files.createDirectories(targetPath.getParent());

        // Copy file to target
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.debug("[LocalStorage] Stored file: {} ({} bytes)", targetPath, file.getSize());

        return storagePath;
    }

    @Override
    public Resource load(String storagePath) throws IOException {
        Path filePath = rootPath.resolve(storagePath).normalize();

        if (!filePath.startsWith(rootPath)) {
            throw new SecurityException("Cannot access file outside root directory");
        }

        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            throw new IOException("File not found: " + storagePath);
        }

        return resource;
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Path filePath = rootPath.resolve(storagePath).normalize();

        if (!filePath.startsWith(rootPath)) {
            throw new SecurityException("Cannot delete file outside root directory");
        }

        Files.deleteIfExists(filePath);
        log.debug("[LocalStorage] Deleted file: {}", filePath);
    }

    @Override
    public boolean exists(String storagePath) {
        Path filePath = rootPath.resolve(storagePath).normalize();
        return filePath.startsWith(rootPath) && Files.exists(filePath);
    }

    @Override
    public String getUrl(String storagePath) {
        // For local storage, return the API download URL
        return "/api/files/download/" + storagePath;
    }
}
