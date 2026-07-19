package com.CodeSphere.backend.dto;

import lombok.Data;

@Data
public class SubmissionDto {
    private Long id;
    private Long questionId;
    private String sourceCode;
    private String language;
}