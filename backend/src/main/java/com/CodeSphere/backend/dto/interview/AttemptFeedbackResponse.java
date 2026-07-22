package com.CodeSphere.backend.dto.interview;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttemptFeedbackResponse {
    private Boolean isCorrect;
    private String correctAnswer;
    private String explanation;
    private Integer score;
}
