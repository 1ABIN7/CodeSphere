package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.AssessmentRecommendationDTO;
import com.CodeSphere.backend.dto.CertificationDTO;
import com.CodeSphere.backend.dto.submission.ExamHistoryDTO;
import com.CodeSphere.backend.dto.submission.SubmissionHistoryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserProfileService {

    Page<SubmissionHistoryDTO> getSubmissionHistory(Long userId, Pageable pageable);

    Page<ExamHistoryDTO> getExamHistory(Long userId, Pageable pageable);

    List<CertificationDTO> getCertifications(Long userId);

    List<AssessmentRecommendationDTO> getRecommendations(Long userId);
}