package com.CodeSphere.backend.scheduler;

import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.model.InAppNotification;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.InAppNotificationRepository;
import com.CodeSphere.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.List;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ResultAnnouncementScheduler {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentRepository assessmentRepository;
    private final InAppNotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 */15 * * * *") // Runs every 15 minutes
    public void announceGradedResults() {
        log.info("[ResultScheduler] Starting periodic result announcement check...");
        List<AssessmentSession> gradedSessions = sessionRepository.findAll().stream()
                .filter(s -> "GRADED".equals(s.getStatus()))
                .toList();

        for (AssessmentSession session : gradedSessions) {
            // Check if an in-app result notification has already been generated
            String titleKey = "Exam Results Published";
            List<InAppNotification> notices = notificationRepository.findByUserIdOrderByCreatedAtDesc(session.getUserId());
            boolean alreadyNotified = notices.stream()
                    .anyMatch(n -> titleKey.equals(n.getTitle()) && n.getMessage().contains(String.valueOf(session.getTotalScore())));

            if (!alreadyNotified) {
                Assessment assessment = assessmentRepository.findById(session.getAssessmentId()).orElse(null);
                String examTitle = assessment != null ? assessment.getTitle() : "Assessment Results";
                double score = session.getTotalScore() != null ? session.getTotalScore() : 0.0;

                log.info("[ResultScheduler] Dispatching batch results notice for session ID: {}, candidate ID: {}", 
                        session.getId(), session.getUserId());
                try {
                    notificationService.notifyCandidateOfResult(session.getUserId(), examTitle, score);
                } catch (Exception e) {
                    log.error("[ResultScheduler] Failed to dispatch results for session ID {}: {}", session.getId(), e.getMessage());
                }
            }
        }
        log.info("[ResultScheduler] Finished periodic result announcement check.");
    }
}
