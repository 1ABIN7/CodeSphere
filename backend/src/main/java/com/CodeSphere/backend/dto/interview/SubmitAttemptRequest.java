package com.CodeSphere.backend.dto.interview;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitAttemptRequest {
    private String userAnswer;
    private Integer timeTakenSeconds;
}
