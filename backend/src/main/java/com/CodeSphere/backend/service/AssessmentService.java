package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.AssessmentResultDto;
import com.CodeSphere.backend.dto.SubmissionDto;
import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Merged core Assessment operational service engine interface contract.
 */
public interface AssessmentService {
    // Teammate's original module methods
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

    // Unique custom business methods brought over from your branch
    Object createBlueprint(Object assessmentDto);
    void start(Long assessmentId, Long userId);
    AssessmentResultDto submit(Long assessmentId, Long userId, SubmissionDto submissionDto);
}