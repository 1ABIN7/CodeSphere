package com.CodeSphere.backend.service;

import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import java.time.LocalDateTime;
import java.util.List;

public interface AssessmentService {
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
}
