package com.CodeSphere.backend.dto.evaluation;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record EvaluatorAssignmentRequest(@NotEmpty List<Long> evaluatorIds) {}
