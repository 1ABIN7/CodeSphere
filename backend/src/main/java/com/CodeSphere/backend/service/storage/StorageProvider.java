package com.CodeSphere.backend.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Abstraction layer for file storage backends.
 *
 * Implementations handle the actual persistence of files,
 * whether on the local filesystem or S3-compatible storage.
 */
public interface StorageProvider {

    /**
     * Store a file and return its storage path.
     *
     * @param file         the uploaded file
     * @param storagePath  the logical path within the storage system
     * @return the actual stored path/key
     */
    String store(MultipartFile file, String storagePath) throws IOException;

    /**
     * Load a stored file as a Spring Resource.
     *
     * @param storagePath the path/key of the stored file
     * @return a Resource for streaming the file content
     */
    Resource load(String storagePath) throws IOException;

    /**
     * Delete a stored file.
     *
     * @param storagePath the path/key of the stored file
     */
    void delete(String storagePath) throws IOException;

    /**
     * Check if a file exists at the given path.
     *
     * @param storagePath the path/key to check
     * @return true if the file exists
     */
    boolean exists(String storagePath);

    /**
     * Get a publicly accessible URL for the file (if supported).
     *
     * @param storagePath the path/key of the stored file
     * @return the URL string, or the storage path itself if URLs aren't supported
     */
    String getUrl(String storagePath);
}
