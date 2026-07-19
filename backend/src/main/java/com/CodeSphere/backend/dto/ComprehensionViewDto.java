package com.CodeSphere.backend.model;

import java.util.List;

public class ComprehensionViewDto {
    private Long passageQuestionId;
    private String passageText;
    private int readingDurationSeconds;
    private String phase; // "READING" or "QUESTIONS"
    private List<Question> subQuestions; // Kept null during Phase 1

    public ComprehensionViewDto(Long passageQuestionId, String passageText, int readingDurationSeconds, String phase, List<Question> subQuestions) {
        this.passageQuestionId = passageQuestionId;
        this.passageText = passageText;
        this.readingDurationSeconds = readingDurationSeconds;
        this.phase = phase;
        this.subQuestions = subQuestions;
    }

    // Getters
    public Long getPassageQuestionId() { return passageQuestionId; }
    public String getPassageText() { return passageText; }
    public int getReadingDurationSeconds() { return readingDurationSeconds; }
    public String getPhase() { return phase; }
    public List<Question> getSubQuestions() { return subQuestions; }
}