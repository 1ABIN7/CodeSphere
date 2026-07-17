package com.codesphere.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_sessions")
public class AssessmentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @ElementCollection
    @CollectionTable(name = "session_question_snapshots", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "question_id")
    private List<Long> questionIdsSnapshot = new ArrayList<>();

    public enum SessionStatus {
        IN_PROGRESS, SUBMITTED, TIMED_OUT
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    public List<Long> getQuestionIdsSnapshot() { return questionIdsSnapshot; }
    public void setQuestionIdsSnapshot(List<Long> questionIdsSnapshot) { this.questionIdsSnapshot = questionIdsSnapshot; }
}