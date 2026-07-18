package com.CodeSphere.backend.service;

import java.util.List;
import java.util.Map;

public interface CandidateReportService {
    Map<String, Object> getCandidateReport(Long userId);
    List<Map<String, Object>> getPerformanceTrend(Long userId);
}
