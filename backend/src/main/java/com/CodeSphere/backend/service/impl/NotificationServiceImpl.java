package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.InAppNotification;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.InAppNotificationRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.EmailService;
import com.CodeSphere.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final InAppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    public void notifyEvaluator(Long evaluatorId, Long answerId) {
        User evaluator = userRepository.findById(evaluatorId).orElse(null);
        if (evaluator == null) return;

        String name = evaluator.getFirstName() != null ? evaluator.getFirstName() : "Reviewer";
        String message = "You have been assigned to grade candidate answer #" + answerId;

        // 1. Create In-App Notification
        sendInAppNotification(evaluatorId, "New Grading Assignment", message);

        // 2. Send HTML Email
        emailService.sendEvaluationAssigned(evaluator.getEmail(), name, answerId);
    }

    @Override
    public void notifyCandidateOfResult(Long candidateId, String examTitle, Double score) {
        User candidate = userRepository.findById(candidateId).orElse(null);
        if (candidate == null) return;

        String name = candidate.getFirstName() != null ? candidate.getFirstName() : "Candidate";
        String message = "Your evaluation results for '" + examTitle + "' are ready. Score: " + score;

        // 1. Create In-App Notification
        sendInAppNotification(candidateId, "Exam Results Published", message);

        // 2. Send HTML Email
        emailService.sendExamResultPublished(candidate.getEmail(), name, examTitle, score);
    }

    @Override
    public void sendInAppNotification(Long userId, String title, String message) {
        InAppNotification notification = InAppNotification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    @Override
    public List<InAppNotification> getNotificationsForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public void markAsRead(Long notificationId) {
        InAppNotification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }
}
