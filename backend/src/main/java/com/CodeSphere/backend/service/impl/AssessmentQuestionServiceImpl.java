package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.entity.AssessmentQuestion;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.service.AssessmentQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentQuestionServiceImpl implements AssessmentQuestionService {

    private final AssessmentQuestionRepository questionRepository;

    @Override
    public AssessmentQuestion addQuestionToSection(AssessmentQuestion aq) {
        List<AssessmentQuestion> existing = questionRepository.findBySectionIdOrderByOrderIndexAsc(aq.getSectionId());
        if (aq.getOrderIndex() == null) {
            aq.setOrderIndex(existing.size() + 1);
        }
        return questionRepository.save(aq);
    }

    @Override
    public List<AssessmentQuestion> getQuestionsBySectionId(Long sectionId) {
        return questionRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);
    }

    @Override
    @Transactional
    public void reorderQuestionsInSection(Long sectionId, List<Long> questionMappingIdsInNewOrder) {
        for (int i = 0; i < questionMappingIdsInNewOrder.size(); i++) {
            Long mappingId = questionMappingIdsInNewOrder.get(i);
            AssessmentQuestion aq = questionRepository.findById(mappingId)
                    .orElseThrow(() -> new IllegalArgumentException("Question mapping not found: " + mappingId));
            if (!aq.getSectionId().equals(sectionId)) {
                throw new IllegalArgumentException("Question mapping " + mappingId + " does not belong to section " + sectionId);
            }
            aq.setOrderIndex(i + 1);
            questionRepository.save(aq);
        }
    }

    @Override
    @Transactional
    public void removeQuestionFromSection(Long id) {
        AssessmentQuestion aq = questionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question mapping not found: " + id));
        
        questionRepository.delete(aq);
        
        // Re-adjust question orders in the same section to close the gap
        List<AssessmentQuestion> remaining = questionRepository.findBySectionIdOrderByOrderIndexAsc(aq.getSectionId());
        for (int i = 0; i < remaining.size(); i++) {
            AssessmentQuestion remAq = remaining.get(i);
            remAq.setOrderIndex(i + 1);
            questionRepository.save(remAq);
        }
    }
}
