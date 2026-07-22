package com.CodeSphere.backend.dto.interview;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartSessionRequest {
    private String sessionType; // PRACTICE, MOCK_INTERVIEW, TIMED_TEST
    private List<Long> categoryIds; // Which categories to include
    private Integer totalQuestions; // Default 10
}
