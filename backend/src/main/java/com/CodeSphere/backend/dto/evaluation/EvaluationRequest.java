package com.CodeSphere.backend.dto.evaluation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class EvaluationRequest {
    @NotNull @DecimalMin("0.0")
    private BigDecimal score;
    private String feedback;
    private Map<String, BigDecimal> rubricScores;
}
