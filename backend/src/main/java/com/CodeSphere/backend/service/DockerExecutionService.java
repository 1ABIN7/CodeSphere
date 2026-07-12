package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.DockerExecutionResult;

public interface DockerExecutionService {
    DockerExecutionResult executeSubmission(Long submissionId, String code, String language, Long problemId);
    DockerExecutionResult runCode(String code, String language, Long problemId);
}
