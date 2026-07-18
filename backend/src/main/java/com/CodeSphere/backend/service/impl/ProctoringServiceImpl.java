package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.ProctoringEvent;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.ProctoringEventRepository;
import com.CodeSphere.backend.service.ProctoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProctoringServiceImpl implements ProctoringService {

    private final ProctoringEventRepository eventRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ProctoringEvent recordEvent(Long sessionId, ProctoringEvent event) {
        event.setSessionId(sessionId);
        ProctoringEvent saved = eventRepository.save(event);

        // Calculate and increment anomaly score in session
        AssessmentSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            double anomalyPoints = calculateAnomalyPoints(event);
            double currentScore = session.getProctoringAnomalyScore() != null ? session.getProctoringAnomalyScore() : 0.0;
            session.setProctoringAnomalyScore(currentScore + anomalyPoints);
            sessionRepository.save(session);
            log.info("Recorded proctoring event for session #{}, type: {}, anomaly increment: {}",
                    sessionId, event.getEventType(), anomalyPoints);
        }

        // Live broadcast proctor event to monitoring admins
        messagingTemplate.convertAndSend("/topic/proctor-events/" + sessionId, saved);

        return saved;
    }

    @Override
    public Map<String, Object> getProctoringReport(Long sessionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        List<ProctoringEvent> events = eventRepository.findBySessionIdOrderByOccurredAtAsc(sessionId);

        // Group breakdown count of events per type
        Map<String, Long> countByType = events.stream()
                .collect(Collectors.groupingBy(ProctoringEvent::getEventType, Collectors.counting()));

        // Map severity counts
        long criticalCount = eventRepository.countBySessionIdAndSeverity(sessionId, "CRITICAL");
        long warningCount = eventRepository.countBySessionIdAndSeverity(sessionId, "WARNING");
        long infoCount = eventRepository.countBySessionIdAndSeverity(sessionId, "INFO");

        Map<String, Object> report = new HashMap<>();
        report.put("sessionId", sessionId);
        report.put("userId", session.getUserId());
        report.put("assessmentId", session.getAssessmentId());
        report.put("totalAnomalyScore", session.getProctoringAnomalyScore());
        report.put("eventCount", events.size());
        report.put("severityBreakdown", Map.of(
                "critical", criticalCount,
                "warning", warningCount,
                "info", infoCount
        ));
        report.put("typeBreakdown", countByType);
        report.put("timeline", events);

        return report;
    }

    private double calculateAnomalyPoints(ProctoringEvent event) {
        double points = 0.0;
        String type = event.getEventType() != null ? event.getEventType().toUpperCase() : "";
        
        switch (type) {
            case "TAB_SWITCH":
                points = 10.0;
                break;
            case "FACE_NOT_FOUND":
                points = 25.0;
                break;
            case "FACE_MULTIPLE":
                points = 30.0;
                break;
            case "VOICE_DETECTED":
                points = 15.0;
                break;
            default:
                points = 5.0;
                break;
        }

        String severity = event.getSeverity() != null ? event.getSeverity().toUpperCase() : "";
        if ("CRITICAL".equals(severity)) {
            points += 20.0;
        } else if ("WARNING".equals(severity)) {
            points += 10.0;
        }

        return points;
    }
}
