package com.CodeSphere.backend.service;

import com.CodeSphere.backend.entity.AssessmentSection;
import java.util.List;

public interface AssessmentSectionService {
    AssessmentSection addSection(AssessmentSection section);
    List<AssessmentSection> getSectionsByAssessmentId(Long assessmentId);
    void reorderSections(Long assessmentId, List<Long> sectionIdsInNewOrder);
    void deleteSection(Long id);
}
