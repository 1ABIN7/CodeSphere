package com.CodeSphere.backend.dto.evaluation;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record ResolveEvaluationRequest(@NotNull BigDecimal score, String feedback) {}
