package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.ProctoringEventRequest;
import com.CodeSphere.backend.dto.file.FileUploadResponse;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.ProctoringConfig;
import com.CodeSphere.backend.model.ProctoringEvent;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.ProctoringConfigRepository;
import com.CodeSphere.backend.repository.ProctoringEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProctoringServiceTest {

    @Mock private ProctoringEventRepository eventRepository;
    @Mock private AssessmentSessionRepository sessionRepository;
    @Mock private ProctoringConfigRepository configRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ProctoringService proctoringService;

    private AssessmentSession session;
    private User candidate;

    @BeforeEach
    void setUp() {
        candidate = new User();
        candidate.setId(10L);

        session = new AssessmentSession();
        session.setId(1L);
        session.setCandidate(candidate);
    }

    // ---- getConfig ----

    @Test
    void getConfig_ReturnsSafeDefaults_WhenNoneConfiguredYet() {
        when(configRepository.findByAssessmentId(5L)).thenReturn(Optional.empty());

        ProctoringConfig config = proctoringService.getConfig(5L);

        assertEquals(5L, config.getAssessmentId());
        assertFalse(config.isEnableWebcam(), "Webcam must default OFF, not silently enabled");
        assertTrue(config.isEnableTabSwitchDetection());
    }

    @Test
    void getConfig_ReturnsStoredConfig_WhenOneExists() {
        ProctoringConfig stored = ProctoringConfig.builder().assessmentId(5L).enableWebcam(true).build();
        when(configRepository.findByAssessmentId(5L)).thenReturn(Optional.of(stored));

        ProctoringConfig config = proctoringService.getConfig(5L);

        assertTrue(config.isEnableWebcam());
        verify(configRepository, never()).save(any());
    }

    // ---- updateConfig ----

    @Test
    void updateConfig_CreatesNew_WhenNoneExistedBefore() {
        ProctoringConfig incoming = ProctoringConfig.builder()
                .enableWebcam(true).enableFaceDetection(true).maxAllowedViolations(3).build();
        when(configRepository.findByAssessmentId(5L)).thenReturn(Optional.empty());
        when(configRepository.save(any(ProctoringConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ProctoringConfig result = proctoringService.updateConfig(5L, incoming);

        assertEquals(5L, result.getAssessmentId());
        assertTrue(result.isEnableWebcam());
        assertEquals(3, result.getMaxAllowedViolations());
    }

    // ---- recordEvent ----

    @Test
    void recordEvent_Success_WhenCallerIsSessionOwner() {
        ProctoringEventRequest request = new ProctoringEventRequest();
        request.setEventType("TAB_SWITCH");
        request.setSeverity("LOW");

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(eventRepository.save(any(ProctoringEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        ProctoringEvent event = proctoringService.recordEvent(1L, 10L, request);

        assertEquals("TAB_SWITCH", event.getEventType());
        assertEquals(session, event.getSession());
    }

    @Test
    void recordEvent_ThrowsException_WhenSessionNotFound() {
        ProctoringEventRequest request = new ProctoringEventRequest();
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> proctoringService.recordEvent(99L, 10L, request));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void recordEvent_ThrowsException_WhenCallerIsNotSessionOwner() {
        ProctoringEventRequest request = new ProctoringEventRequest();
        request.setEventType("TAB_SWITCH");
        request.setSeverity("LOW");
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class,
                () -> proctoringService.recordEvent(1L, 999L, request));
        verify(eventRepository, never()).save(any());
    }

    // ---- uploadSnapshot ----

    @Test
    void uploadSnapshot_Success_WhenCallerIsSessionOwner() {
        MockMultipartFile file = new MockMultipartFile("file", "webcam.jpg", "image/jpeg", new byte[]{1, 2, 3});
        FileUploadResponse uploadResponse = FileUploadResponse.builder().fileUrl("/files/abc123").build();

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(fileStorageService.upload(file, "PROCTORING", 1L, 10L)).thenReturn(uploadResponse);
        when(eventRepository.save(any(ProctoringEvent.class))).thenAnswer(inv -> inv.getArgument(0));

        ProctoringEvent event = proctoringService.uploadSnapshot(1L, 10L, file, "WEBCAM_SNAPSHOT");

        assertEquals("/files/abc123", event.getScreenshotUrl());
        assertEquals("INFO", event.getSeverity());
    }

    @Test
    void uploadSnapshot_ThrowsException_WhenCallerIsNotSessionOwner() {
        MockMultipartFile file = new MockMultipartFile("file", "webcam.jpg", "image/jpeg", new byte[]{1});
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class,
                () -> proctoringService.uploadSnapshot(1L, 999L, file, "WEBCAM_SNAPSHOT"));
        verify(fileStorageService, never()).upload(any(), any(), any(), any());
    }

    // ---- getSessionEvents ----

    @Test
    void getSessionEvents_ReturnsEventsInDescendingTimestampOrder() {
        ProctoringEvent event = ProctoringEvent.builder().id(1L).session(session).eventType("TAB_SWITCH").build();
        when(eventRepository.findBySessionIdOrderByTimestampDesc(1L)).thenReturn(List.of(event));

        List<ProctoringEvent> events = proctoringService.getSessionEvents(1L);

        assertEquals(1, events.size());
    }

    // ---- flagSession: documents a real, currently-unimplemented gap ----

    /**
     * flagSession() currently does nothing — its body is an empty lambda with only comments
     * ("Need to use JDBC or extend the entity to include these fields if they aren't there yet").
     * This test locks in that known-incomplete behavior so it fails loudly (instead of silently)
     * the moment someone implements real flagging logic, forcing this test to be rewritten
     * alongside the fix rather than left stale.
     */
    @Test
    void flagSession_CurrentlyDoesNothing_NoExceptionButNoPersistence() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertDoesNotThrow(() -> proctoringService.flagSession(1L, "Suspicious tab-switch pattern"));

        // No save/update call exists anywhere in the current implementation.
        verifyNoInteractions(eventRepository);
    }
}
