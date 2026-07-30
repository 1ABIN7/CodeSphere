package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.ProctoringEventRequest;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.ProctoringConfig;
import com.CodeSphere.backend.model.ProctoringEvent;
import com.CodeSphere.backend.dto.file.FileUploadResponse;
import com.CodeSphere.backend.dto.admin.ProctoringEventSummary;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.ProctoringConfigRepository;
import com.CodeSphere.backend.repository.ProctoringEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProctoringService {

    private final ProctoringEventRepository eventRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final ProctoringConfigRepository configRepository;
    private final FileStorageService fileStorageService;

    public ProctoringConfig getConfig(Long assessmentId) {
        return configRepository.findByAssessmentId(assessmentId)
                .orElse(ProctoringConfig.builder()
                        .assessmentId(assessmentId)
                        .enableWebcam(false)
                        .enableTabSwitchDetection(true)
                        .build());
    }

    @Transactional
    public ProctoringConfig updateConfig(Long assessmentId, ProctoringConfig config) {
        ProctoringConfig existing = configRepository.findByAssessmentId(assessmentId)
                .orElse(ProctoringConfig.builder().assessmentId(assessmentId).build());
        
        existing.setEnableWebcam(config.isEnableWebcam());
        existing.setEnableScreenRecording(config.isEnableScreenRecording());
        existing.setEnableTabSwitchDetection(config.isEnableTabSwitchDetection());
        existing.setEnableFaceDetection(config.isEnableFaceDetection());
        existing.setEnableAudioDetection(config.isEnableAudioDetection());
        existing.setMaxAllowedViolations(config.getMaxAllowedViolations());
        existing.setWarningThreshold(config.getWarningThreshold());
        existing.setAutoDisqualify(config.isAutoDisqualify());
        
        return configRepository.save(existing);
    }

    @Transactional
    public ProctoringEvent recordEvent(Long sessionId, Long userId, ProctoringEventRequest request) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        if (!session.getCandidate().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        ProctoringEvent event = ProctoringEvent.builder()
                .session(session)
                .eventType(request.getEventType())
                .severity(request.getSeverity())
                .metadata(request.getMetadata())
                .timestamp(LocalDateTime.now())
                .build();
        
        event = eventRepository.save(event);
        
        // Update session violation count if HIGH or CRITICAL
        if ("HIGH".equals(request.getSeverity()) || "CRITICAL".equals(request.getSeverity())) {
            jdbcUpdateSessionViolationCount(sessionId);
        }
        
        return event;
    }

    @Transactional
    public ProctoringEvent uploadSnapshot(Long sessionId, Long userId, MultipartFile file, String eventType) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        if (!session.getCandidate().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        
        // Upload snapshot using storage service
        FileUploadResponse uploadResponse = fileStorageService.upload(file, "PROCTORING", sessionId, userId);
        
        ProctoringEvent event = ProctoringEvent.builder()
                .session(session)
                .eventType(eventType)
                .severity("INFO") // snapshots are just info unless tied to a specific violation
                .screenshotUrl(uploadResponse.getFileUrl())
                .timestamp(LocalDateTime.now())
                .build();
                
        return eventRepository.save(event);
    }
    
    public List<ProctoringEvent> getSessionEvents(Long sessionId) {
        return eventRepository.findBySessionIdOrderByTimestampDesc(sessionId);
    }

    @Transactional(readOnly = true)
    public List<ProctoringEventSummary> getRecentEvents() {
        return eventRepository.findTop50ByOrderByTimestampDesc().stream().map(event -> {
            AssessmentSession session = event.getSession();
            String candidate = session.getCandidate().getFullName();
            if (candidate == null || candidate.isBlank()) candidate = session.getCandidate().getUsername();
            return new ProctoringEventSummary(event.getId(), session.getId(), session.getAssessmentId(), candidate,
                    event.getEventType(), event.getSeverity(), event.getTimestamp(), event.getMetadata());
        }).toList();
    }
    
    @Transactional
    public void flagSession(Long sessionId, String reason) {
        // Implement flagging logic via JDBC or entity update depending on your schema.
        // Assuming we added is_flagged and flag_reason to assessment_sessions
        sessionRepository.findById(sessionId).ifPresent(s -> {
            // Need to use JDBC or extend the entity to include these fields if they aren't there yet
        });
    }
    
    private void jdbcUpdateSessionViolationCount(Long sessionId) {
        // We do this manually because the entity might not be fully mapped with these V9 columns yet
        // In a real scenario you'd update the AssessmentSession entity
        // jdbcTemplate.update("UPDATE assessment_sessions SET violation_count = violation_count + 1 WHERE id = ?", sessionId);
    }
}
