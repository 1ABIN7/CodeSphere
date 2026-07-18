package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.InAppNotification;
import java.util.List;

public interface NotificationService {
    void notifyEvaluator(Long evaluatorId, Long answerId);
    void notifyCandidateOfResult(Long candidateId, String examTitle, Double score);
    void sendInAppNotification(Long userId, String title, String message);
    List<InAppNotification> getNotificationsForUser(Long userId);
    void markAsRead(Long notificationId);
}
