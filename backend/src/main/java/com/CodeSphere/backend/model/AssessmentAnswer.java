package com.CodeSphere.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "session_answers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id", "question_id"})
})
public class AssessmentAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(columnDefinition = "TEXT")
    private String selectedAnswer;

    private LocalDateTime updatedAt;

    @Column(name = "evaluation_status")
    private String evaluationStatus;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "evaluator_feedback", columnDefinition = "TEXT")
    private String evaluatorFeedback;

    @Column(name = "evaluated_by")
    private Long evaluatedBy;

    @Column(name = "evaluated_at")
    private OffsetDateTime evaluatedAt;

    @Column(name = "file_url", length = 1024)
    private String fileUrl;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getSelectedAnswer() { return selectedAnswer; }
    public void setSelectedAnswer(String selectedAnswer) { this.selectedAnswer = selectedAnswer; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getEvaluationStatus() { return evaluationStatus; }
    public void setEvaluationStatus(String evaluationStatus) { this.evaluationStatus = evaluationStatus; }
    public BigDecimal getScore() { return score; }
    public void setScore(BigDecimal score) { this.score = score; }
    public String getEvaluatorFeedback() { return evaluatorFeedback; }
    public void setEvaluatorFeedback(String evaluatorFeedback) { this.evaluatorFeedback = evaluatorFeedback; }
    public Long getEvaluatedBy() { return evaluatedBy; }
    public void setEvaluatedBy(Long evaluatedBy) { this.evaluatedBy = evaluatedBy; }
    public OffsetDateTime getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(OffsetDateTime evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
}
