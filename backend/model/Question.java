package com.codesphere.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Content is required")
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private String category;
    private String type;
    private String difficulty;

    @ElementCollection
    @CollectionTable(name = "question_tags", joinColumns = @JoinColumn(name = "question_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    // Workflow Status Fields (Step 2)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus status = ApprovalStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String rejectionFeedback;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public ApprovalStatus getStatus() { return status; }
    public void setStatus(ApprovalStatus status) { this.status = status; }

    public String getRejectionFeedback() { return rejectionFeedback; }
    public void setRejectionFeedback(String rejectionFeedback) { this.rejectionFeedback = rejectionFeedback; }

    @Column(name = "correct_answers")
    private String correctAnswers; // Store as comma-separated values for MCQ_MULTI (e.g., "A,C") or single letter ("B")

    @Column(name = "points", nullable = false)
    private Integer points = 0;

    @Column(name = "negative_score", nullable = false)
    private Integer negativeScore = 0;

    @Column(name = "question_type")
    private String questionType; // "MCQ_SINGLE" or "MCQ_MULTI"

    @Column(name = "min_word_count")
    private Integer minWordCount = 0;

    @Column(name = "max_word_count")
    private Integer maxWordCount = 2000;

    @Column(name = "passage_text", columnDefinition = "TEXT")
    private String passageText;

    @Column(name = "reading_duration_seconds")
    private Integer readingDurationSeconds = 0; // Phase 1 restriction window

    @Column(name = "parent_question_id")
    private Long parentQuestionId; // Links a sub-question back to the main passage question

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_question_id")
    private List<Question> subQuestions = new ArrayList<>();
}