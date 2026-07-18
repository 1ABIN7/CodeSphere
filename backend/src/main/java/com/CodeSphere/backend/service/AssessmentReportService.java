package com.CodeSphere.backend.service;

import java.util.Map;

public interface AssessmentReportService {
    Map<String, Object> getAssessmentReport(Long assessmentId);
    byte[] exportAssessmentReportCSV(Long assessmentId);
    byte[] exportAssessmentReportPDF(Long assessmentId);
}
