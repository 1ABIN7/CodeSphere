package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.entity.AssessmentSection;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.repository.AssessmentSectionRepository;
import com.CodeSphere.backend.service.AssessmentSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentSectionServiceImpl implements AssessmentSectionService {

    private final AssessmentSectionRepository sectionRepository;
    private final AssessmentQuestionRepository questionRepository;

    @Override
    public AssessmentSection addSection(AssessmentSection section) {
        List<AssessmentSection> existing = sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(section.getAssessmentId());
        if (section.getSectionOrder() == null) {
            section.setSectionOrder(existing.size() + 1);
        }
        return sectionRepository.save(section);
    }

    @Override
    public List<AssessmentSection> getSectionsByAssessmentId(Long assessmentId) {
        return sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(assessmentId);
    }

    @Override
    @Transactional
    public void reorderSections(Long assessmentId, List<Long> sectionIdsInNewOrder) {
        for (int i = 0; i < sectionIdsInNewOrder.size(); i++) {
            Long secId = sectionIdsInNewOrder.get(i);
            AssessmentSection sec = sectionRepository.findById(secId)
                    .orElseThrow(() -> new IllegalArgumentException("Section not found: " + secId));
            if (!sec.getAssessmentId().equals(assessmentId)) {
                throw new IllegalArgumentException("Section " + secId + " does not belong to assessment " + assessmentId);
            }
            sec.setSectionOrder(i + 1);
            sectionRepository.save(sec);
        }
    }

    @Override
    @Transactional
    public void deleteSection(Long id) {
        AssessmentSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Section not found: " + id));
        
        // 1. Delete all mapped questions in this section
        questionRepository.deleteBySectionId(id);
        
        // 2. Delete the section
        sectionRepository.delete(section);
        
        // 3. Re-adjust section orders to close any gaps
        List<AssessmentSection> remaining = sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(section.getAssessmentId());
        for (int i = 0; i < remaining.size(); i++) {
            AssessmentSection sec = remaining.get(i);
            sec.setSectionOrder(i + 1);
            sectionRepository.save(sec);
        }
    }
}
