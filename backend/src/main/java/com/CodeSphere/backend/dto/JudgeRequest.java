package com.CodeSphere.backend.dto;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JudgeRequest implements Serializable {
    private Long submissionId;
    private Long problemId;
    private String code;
    private String language;
    private boolean isRunOnly;
}
