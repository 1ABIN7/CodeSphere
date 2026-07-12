package com.CodeSphere.backend.service;

import com.CodeSphere.backend.entity.AssessmentQuestion;
import java.util.List;

public interface AssessmentQuestionService {
    AssessmentQuestion addQuestionToSection(AssessmentQuestion aq);
    List<AssessmentQuestion> getQuestionsBySectionId(Long sectionId);
    void reorderQuestionsInSection(Long sectionId, List<Long> questionMappingIdsInNewOrder);
    void removeQuestionFromSection(Long id);
}
