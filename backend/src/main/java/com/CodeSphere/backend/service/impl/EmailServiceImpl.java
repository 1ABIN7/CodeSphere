package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    private void sendHtmlEmail(String to, String subject, String templateName, Context context) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject(subject);
            
            String htmlContent = templateEngine.process(templateName, context);
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("Email sent successfully to {} with subject: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {} with template {}: {}. Continuing execution gracefully.", to, templateName, e.getMessage());
        }
    }

    @Override
    public void sendRegistrationConfirmation(String email, String name) {
        Context context = new Context();
        context.setVariable("name", name);
        sendHtmlEmail(email, "Welcome to CodeSphere!", "mail/registration-confirmation", context);
    }

    @Override
    public void sendExamResultPublished(String email, String candidateName, String examTitle, Double score) {
        Context context = new Context();
        context.setVariable("name", candidateName);
        context.setVariable("examTitle", examTitle);
        context.setVariable("score", score);
        sendHtmlEmail(email, "Your CodeSphere Exam Results are Available", "mail/exam-result-published", context);
    }

    @Override
    public void sendPasswordReset(String email, String resetToken) {
        Context context = new Context();
        context.setVariable("token", resetToken);
        sendHtmlEmail(email, "Reset Your CodeSphere Password", "mail/password-reset", context);
    }

    @Override
    public void sendEvaluationAssigned(String email, String reviewerName, Long answerId) {
        Context context = new Context();
        context.setVariable("name", reviewerName);
        context.setVariable("answerId", answerId);
        sendHtmlEmail(email, "New Candidate Answer Assigned for Evaluation", "mail/evaluation-assigned", context);
    }

    @Override
    public void sendEvaluationComplete(String email, String candidateName, String examTitle) {
        Context context = new Context();
        context.setVariable("name", candidateName);
        context.setVariable("examTitle", examTitle);
        sendHtmlEmail(email, "Your Exam Evaluation is Complete", "mail/evaluation-complete", context);
    }
}
