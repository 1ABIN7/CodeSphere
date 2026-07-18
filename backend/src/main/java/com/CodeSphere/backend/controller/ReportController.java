package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.service.AssessmentReportService;
import com.CodeSphere.backend.service.CandidateReportService;
import com.CodeSphere.backend.service.PlatformAnalyticsService;
import com.CodeSphere.backend.service.QuestionAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final CandidateReportService candidateReportService;
    private final AssessmentReportService assessmentReportService;
    private final QuestionAnalyticsService questionAnalyticsService;
    private final PlatformAnalyticsService platformAnalyticsService;

    @GetMapping("/reports/candidates/{userId}")
    public ResponseEntity<Map<String, Object>> getCandidateReport(@PathVariable Long userId) {
        return ResponseEntity.ok(candidateReportService.getCandidateReport(userId));
    }

    @GetMapping("/reports/candidates/{userId}/performance-trend")
    public ResponseEntity<List<Map<String, Object>>> getPerformanceTrend(@PathVariable Long userId) {
        return ResponseEntity.ok(candidateReportService.getPerformanceTrend(userId));
    }

    @GetMapping("/reports/assessments/{id}")
    public ResponseEntity<Map<String, Object>> getAssessmentReport(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentReportService.getAssessmentReport(id));
    }

    @GetMapping("/reports/assessments/{id}/export")
    public ResponseEntity<byte[]> exportAssessmentReport(
            @PathVariable Long id,
            @RequestParam String format) {
        if ("csv".equalsIgnoreCase(format)) {
            byte[] csvBytes = assessmentReportService.exportAssessmentReportCSV(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assessment_report_" + id + ".csv")
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvBytes);
        } else if ("pdf".equalsIgnoreCase(format)) {
            byte[] pdfBytes = assessmentReportService.exportAssessmentReportPDF(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=assessment_report_" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/reports/questions/{id}/analytics")
    public ResponseEntity<Map<String, Object>> getQuestionAnalytics(@PathVariable Long id) {
        return ResponseEntity.ok(questionAnalyticsService.getQuestionAnalytics(id));
    }

    @GetMapping("/reports/assessments/{id}/difficulty-analysis")
    public ResponseEntity<Map<String, Object>> getDifficultyAnalysis(@PathVariable Long id) {
        return ResponseEntity.ok(questionAnalyticsService.getDifficultyAnalysis(id));
    }

    @GetMapping("/analytics/platform")
    public ResponseEntity<Map<String, Object>> getPlatformAnalytics() {
        return ResponseEntity.ok(platformAnalyticsService.getPlatformAnalytics());
    }
}
