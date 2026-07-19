package com.CodeSphere.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String category;
    private String type; // For getType()
    private String questionType; // For getQuestionType()
    private String difficulty;

    @ElementCollection
    private List<String> tags;

    @Enumerated(EnumType.STRING)
    private ApprovalStatus status;
    private String rejectionFeedback;

    @Column(columnDefinition = "TEXT")
    private String passageText;
    private Integer readingDurationSeconds;
    private Long parentQuestionId;

    private Double points;
    private Double negativeScore;
    private Integer minWordCount;
    private Integer maxWordCount;

    @ElementCollection
    private List<String> correctAnswers;
}