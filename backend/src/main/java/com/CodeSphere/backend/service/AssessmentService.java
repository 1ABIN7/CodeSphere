package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.AssessmentResultDTO;
import com.CodeSphere.backend.dto.SubmissionDTO;
import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import com.CodeSphere.backend.model.AssessmentSession;

import java.time.LocalDateTime;
import java.util.List;

public interface AssessmentService {

    // --- Teammate's Management Methods ---
    Assessment createAssessment(Assessment assessment);
    Assessment updateAssessment(Long id, Assessment assessmentDetails);
    void deleteAssessment(Long id);
    Assessment getAssessmentById(Long id);
    List<Assessment> getAllAssessments();
    Assessment cloneAssessment(Long id);
    Assessment publishAssessment(Long id);
    Assessment unpublishAssessment(Long id);
    AssessmentAssignment assignAssessment(Long id, Long userId, LocalDateTime deadline);
    List<AssessmentAssignment> getAssignedCandidates(Long id);

    // --- Your Active Execution & Session Methods (Merged) ---
    Object findById(Long assessmentId);
    Object createBlueprint(Object assessmentDto);
    AssessmentSession start(Long assessmentId, Long userId);
    AssessmentResultDTO submit(Long assessmentId, Long userId, SubmissionDTO submissionDto);
}
