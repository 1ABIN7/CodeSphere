package com.CodeSphere.backend.service;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import com.CodeSphere.backend.config.StorageConfig;
import com.CodeSphere.backend.dto.file.FileUploadResponse;
import com.CodeSphere.backend.model.FileAttachment;
import com.CodeSphere.backend.repository.FileAttachmentRepository;
import com.CodeSphere.backend.service.storage.StorageProvider;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock private StorageProvider storageProvider;
    @Mock private FileAttachmentRepository fileAttachmentRepository;
    @Mock private StorageConfig storageConfig;

    @InjectMocks
    private FileStorageService fileStorageService;

    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        validFile = new MockMultipartFile(
                "file", "solution.pdf", "application/pdf", "dummy content".getBytes());
    }

    // ---- Validation guards ----

    @Test
    void upload_ThrowsException_WhenFileIsEmpty() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.upload(emptyFile, "SUBMISSION", 1L, 10L));

        assertTrue(exception.getMessage().contains("empty"));
        verify(fileAttachmentRepository, never()).save(any());
    }

    @Test
    void upload_ThrowsException_WhenFileExceedsMaxSize() {
        when(storageConfig.getMaxFileSize()).thenReturn(5L); // absurdly small limit to force the guard

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.upload(validFile, "SUBMISSION", 1L, 10L));

        assertTrue(exception.getMessage().contains("exceeds maximum allowed size"));
        verify(fileAttachmentRepository, never()).save(any());
    }

    @Test
    void upload_ThrowsException_WhenContentTypeNotAllowed() {
        when(storageConfig.getMaxFileSize()).thenReturn(52428800L);
        when(storageConfig.getAllowedContentTypes()).thenReturn(List.of("image/png", "image/jpeg"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> fileStorageService.upload(validFile, "SUBMISSION", 1L, 10L));

        assertTrue(exception.getMessage().contains("File type not allowed"));
        verify(fileAttachmentRepository, never()).save(any());
    }

    @Test
    void upload_Allowed_WhenAllowedContentTypesListIsEmpty() throws IOException {
        when(storageConfig.getMaxFileSize()).thenReturn(52428800L);
        when(storageConfig.getAllowedContentTypes()).thenReturn(List.of());
        when(storageConfig.getActiveBackend()).thenReturn("local");
        when(storageProvider.store(eq(validFile), anyString())).thenReturn("submission/1/abc123.pdf");
        when(storageProvider.getUrl("submission/1/abc123.pdf")).thenReturn("/files/abc123.pdf");
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(inv -> {
            FileAttachment a = inv.getArgument(0);
            a.setId(500L);
            a.setCreatedAt(OffsetDateTime.now());
            return a;
        });

        FileUploadResponse response = fileStorageService.upload(validFile, "SUBMISSION", 1L, 10L);

        assertEquals(500L, response.getFileId());
        assertEquals("/files/abc123.pdf", response.getFileUrl());
    }

    // ---- Successful upload: backend selection + checksum ----

    @Test
    void upload_Success_TagsAttachmentWithMinioBackend_WhenConfiguredAsActive() throws IOException {
        when(storageConfig.getMaxFileSize()).thenReturn(52428800L);
        when(storageConfig.getAllowedContentTypes()).thenReturn(List.of());
        when(storageConfig.getActiveBackend()).thenReturn("minio");
        when(storageProvider.store(eq(validFile), anyString())).thenReturn("submission/1/abc123.pdf");
        when(storageProvider.getUrl(anyString())).thenReturn("https://minio.example/abc123.pdf");
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenAnswer(inv -> {
            FileAttachment a = inv.getArgument(0);
            a.setId(501L);
            a.setCreatedAt(OffsetDateTime.now());
            return a;
        });

        FileUploadResponse response = fileStorageService.upload(validFile, "SUBMISSION", 1L, 10L);

        assertEquals("MINIO", response.getStorageBackend());
        assertNotNull(response.getChecksum(), "Checksum should be computed for every upload");
    }

    @Test
    void upload_WrapsIOException_AsRuntimeException_WhenStorageFails() throws IOException {
        when(storageConfig.getMaxFileSize()).thenReturn(52428800L);
        when(storageConfig.getAllowedContentTypes()).thenReturn(List.of());
        when(storageProvider.store(eq(validFile), anyString())).thenThrow(new IOException("disk full"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> fileStorageService.upload(validFile, "SUBMISSION", 1L, 10L));

        assertTrue(exception.getMessage().contains("Failed to store file"));
        verify(fileAttachmentRepository, never()).save(any());
    }

    // ---- download ----

    @Test
    void download_Success_WhenFileExists() throws IOException {
        FileAttachment attachment = FileAttachment.builder().id(1L).filePath("submission/1/abc.pdf").build();
        Resource resource = new ByteArrayResource("content".getBytes());
        when(fileAttachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));
        when(storageProvider.load("submission/1/abc.pdf")).thenReturn(resource);

        Resource result = fileStorageService.download(1L);

        assertEquals(resource, result);
    }

    @Test
    void download_ThrowsException_WhenFileNotFound() {
        when(fileAttachmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> fileStorageService.download(99L));
    }

    // ---- delete ----

    @Test
    void delete_Success_RemovesMetadataAndPhysicalFile() throws IOException {
        FileAttachment attachment = FileAttachment.builder().id(1L).filePath("submission/1/abc.pdf").build();
        when(fileAttachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));

        fileStorageService.delete(1L);

        verify(storageProvider).delete("submission/1/abc.pdf");
        verify(fileAttachmentRepository).delete(attachment);
    }

    @Test
    void delete_StillRemovesMetadata_WhenPhysicalDeleteFails() throws IOException {
        FileAttachment attachment = FileAttachment.builder().id(1L).filePath("submission/1/abc.pdf").build();
        when(fileAttachmentRepository.findById(1L)).thenReturn(Optional.of(attachment));
        doThrow(new IOException("file already gone")).when(storageProvider).delete("submission/1/abc.pdf");

        assertDoesNotThrow(() -> fileStorageService.delete(1L));

        // Metadata cleanup must proceed even if the physical file is already missing
        verify(fileAttachmentRepository).delete(attachment);
    }

    @Test
    void delete_ThrowsException_WhenFileNotFound() {
        when(fileAttachmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> fileStorageService.delete(99L));
        verify(fileAttachmentRepository, never()).delete(any(FileAttachment.class));
    }

    // ---- listByEntity ----

    @Test
    void listByEntity_UppercasesEntityType_BeforeQuerying() {
        when(fileAttachmentRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("SUBMISSION", 1L))
                .thenReturn(List.of());

        fileStorageService.listByEntity("submission", 1L);

        verify(fileAttachmentRepository).findByEntityTypeAndEntityIdOrderByCreatedAtDesc("SUBMISSION", 1L);
    }
}
