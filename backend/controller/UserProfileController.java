package com.codesphere.backend.controller;

import com.codesphere.backend.dto.AssessmentRecommendationDTO;
import com.codesphere.backend.dto.CertificationDTO;
import com.codesphere.backend.dto.ExamHistoryDTO;
import com.codesphere.backend.dto.SubmissionHistoryDTO;
import com.codesphere.backend.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * GET /api/v1/users/me/submissions
     * Retrieves the paginated sequence of all code/answer submissions.
     */
    @GetMapping("/submissions")
    public ResponseEntity<Page<SubmissionHistoryDTO>> getMySubmissions(
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(userProfileService.getSubmissionHistory(userId, pageable));
    }

    /**
     * GET /api/v1/users/me/exams
     * Retrieves complete historical exam attempts, scores, and completion statuses.
     */
    @GetMapping("/exams")
    public ResponseEntity<Page<ExamHistoryDTO>> getMyExamHistory(
            @PageableDefault(size = 10) Pageable pageable) {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(userProfileService.getExamHistory(userId, pageable));
    }

    /**
     * GET /api/v1/users/me/certifications
     * Retrieves all verified credentials/certificates earned by the user.
     */
    @GetMapping("/certifications")
    public ResponseEntity<List<CertificationDTO>> getMyCertifications() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(userProfileService.getCertifications(userId));
    }

    /**
     * GET /api/v1/users/me/recommendations
     * Recommends new assessments based on the user's historic performance and weaker areas.
     */
    @GetMapping("/recommendations")
    public ResponseEntity<List<AssessmentRecommendationDTO>> getRecommendations() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(userProfileService.getRecommendations(userId));
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        return (Long) authentication.getPrincipal();
    }
}