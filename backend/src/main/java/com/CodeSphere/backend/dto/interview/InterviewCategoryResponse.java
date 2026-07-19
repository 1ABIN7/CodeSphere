package com.CodeSphere.backend.dto.interview;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewCategoryResponse {
    private Long id;
    private String name;
    private String displayName;
    private String description;
    private String icon;
    private String color;
}
