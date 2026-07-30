package com.CodeSphere.backend.dto;

import java.util.List;

public record SqlRunResponse(String status, int passedTestCases, int totalTestCases,
                             double scorePercent, List<CaseResult> cases, String message) {
    public record CaseResult(String name, boolean passed, int rowCount, String message) {}
}
