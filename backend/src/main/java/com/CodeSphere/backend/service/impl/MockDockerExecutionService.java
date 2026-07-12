package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.dto.DockerExecutionResult;
import com.CodeSphere.backend.service.DockerExecutionService;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class MockDockerExecutionService implements DockerExecutionService {

    private final Random random = new Random();

    @Override
    public DockerExecutionResult executeSubmission(Long submissionId, String code, String language, Long problemId) {
        // Emulate Docker code execution.
        try {
            Thread.sleep(1500); // simulate running tests
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Mock different outcomes
        int outcome = random.nextInt(10);
        if (outcome < 7) {
            return DockerExecutionResult.builder()
                    .verdict("ACCEPTED")
                    .execTime(150 + random.nextInt(100))
                    .execMemory(12400 + random.nextInt(5000))
                    .build();
        } else if (outcome < 8) {
            return DockerExecutionResult.builder()
                    .verdict("WRONG_ANSWER")
                    .execTime(180 + random.nextInt(50))
                    .execMemory(13000 + random.nextInt(2000))
                    .errorMessage("Test Case #3 failed: expected '4' but got '5'")
                    .build();
        } else {
            return DockerExecutionResult.builder()
                    .verdict("TIME_LIMIT_EXCEEDED")
                    .execTime(1005)
                    .execMemory(15000)
                    .build();
        }
    }

    @Override
    public DockerExecutionResult runCode(String code, String language, Long problemId) {
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return DockerExecutionResult.builder()
                .verdict("ACCEPTED")
                .execTime(120)
                .execMemory(10000)
                .build();
    }
}
