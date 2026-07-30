package com.CodeSphere.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SqlRunRequest(@NotNull Long sessionId, @NotNull Long questionId, @NotBlank String sql) {}
