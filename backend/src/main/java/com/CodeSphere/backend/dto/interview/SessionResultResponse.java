package com.CodeSphere.backend.dto.interview;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResultResponse {
    private Long sessionId;
    private String sessionType;
    private String status;
    private Integer score;
    private Integer maxScore;
    private Integer questionsAnswered;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private List<AttemptDetail> attempts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttemptDetail {
        private Long questionId;
        private String questionText;
        private String questionType;
        private String userAnswer;
        private String correctAnswer;
        private String explanation;
        private Boolean isCorrect;
        private Integer timeTakenSeconds;
        private Integer score;
    }
}
