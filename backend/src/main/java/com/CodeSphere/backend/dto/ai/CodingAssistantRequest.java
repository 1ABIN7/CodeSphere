package com.CodeSphere.backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CodingAssistantRequest(
        @NotBlank(message = "Enter a coding question first.")
        @Size(max = 6000, message = "Keep each message under 6,000 characters.")
        String message
) { }
