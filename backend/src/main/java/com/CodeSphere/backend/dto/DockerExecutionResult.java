package com.CodeSphere.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DockerExecutionResult {
    private String verdict; // ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED, etc.
    private Integer execTime; // ms
    private Integer execMemory; // KB
    private String errorMessage;
}
