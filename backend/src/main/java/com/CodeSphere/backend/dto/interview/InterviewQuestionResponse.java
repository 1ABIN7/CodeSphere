package com.CodeSphere.backend.dto.interview;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewQuestionResponse {
    private Long id;
    private Long categoryId;
    private String questionType;
    private String difficulty;
    private String topic;
    private String questionText;
    private List<Object> options;
    private Integer timeLimitSeconds;
}
