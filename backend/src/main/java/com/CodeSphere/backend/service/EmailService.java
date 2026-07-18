package com.CodeSphere.backend.service;

public interface EmailService {
    void sendRegistrationConfirmation(String email, String name);
    void sendExamResultPublished(String email, String candidateName, String examTitle, Double score);
    void sendPasswordReset(String email, String resetToken);
    void sendEvaluationAssigned(String email, String reviewerName, Long answerId);
    void sendEvaluationComplete(String email, String candidateName, String examTitle);
}
