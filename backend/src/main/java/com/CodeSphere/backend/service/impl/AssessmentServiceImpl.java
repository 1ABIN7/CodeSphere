package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import com.CodeSphere.backend.entity.AssessmentQuestion;
import com.CodeSphere.backend.entity.AssessmentSection;
import com.CodeSphere.backend.repository.AssessmentAssignmentRepository;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSectionRepository;
import com.CodeSphere.backend.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentSectionRepository sectionRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentAssignmentRepository assignmentRepository;

    @Override
    public Assessment createAssessment(Assessment assessment) {
        assessment.setPublished(false);
        return assessmentRepository.save(assessment);
    }

    @Override
    public Assessment updateAssessment(Long id, Assessment assessmentDetails) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id " + id));
        
        assessment.setTitle(assessmentDetails.getTitle());
        assessment.setDescription(assessmentDetails.getDescription());
        assessment.setAssessmentType(assessmentDetails.getAssessmentType());
        assessment.setDurationMinutes(assessmentDetails.getDurationMinutes());
        assessment.setStartTime(assessmentDetails.getStartTime());
        assessment.setEndTime(assessmentDetails.getEndTime());
        assessment.setPassingScore(assessmentDetails.getPassingScore());
        assessment.setInstructions(assessmentDetails.getInstructions());
        assessment.setShuffleQuestions(assessmentDetails.isShuffleQuestions());
        assessment.setShuffleOptions(assessmentDetails.isShuffleOptions());
        assessment.setAllowResume(assessmentDetails.isAllowResume());
        assessment.setCertifying(assessmentDetails.isCertifying());
        assessment.setAccessCode(assessmentDetails.getAccessCode());
        
        return assessmentRepository.save(assessment);
    }

    @Override
    @Transactional
    public void deleteAssessment(Long id) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id " + id));
        questionRepository.deleteByAssessmentId(id);
        sectionRepository.deleteByAssessmentId(id);
        assessmentRepository.delete(assessment);
    }

    @Override
    public Assessment getAssessmentById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id " + id));
    }

    @Override
    public List<Assessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }

    @Override
    @Transactional
    public Assessment cloneAssessment(Long id) {
        Assessment original = getAssessmentById(id);
        
        // 1. Clone assessment metadata
        Assessment cloned = Assessment.builder()
                .title(original.getTitle() + " - Clone")
                .description(original.getDescription())
                .assessmentType(original.getAssessmentType())
                .organizationId(original.getOrganizationId())
                .createdBy(original.getCreatedBy())
                .durationMinutes(original.getDurationMinutes())
                .startTime(original.getStartTime())
                .endTime(original.getEndTime())
                .passingScore(original.getPassingScore())
                .instructions(original.getInstructions())
                .shuffleQuestions(original.isShuffleQuestions())
                .shuffleOptions(original.isShuffleOptions())
                .allowResume(original.isAllowResume())
                .isCertifying(original.isCertifying())
                .isPublished(false) // cloned assessment is unpublished by default
                .accessCode(original.getAccessCode())
                .build();
        
        Assessment savedClone = assessmentRepository.save(cloned);
        
        // 2. Clone sections
        List<AssessmentSection> originalSections = sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(original.getId());
        for (AssessmentSection origSec : originalSections) {
            AssessmentSection clonedSec = AssessmentSection.builder()
                    .assessmentId(savedClone.getId())
                    .title(origSec.getTitle())
                    .sectionOrder(origSec.getSectionOrder())
                    .timeLimitMinutes(origSec.getTimeLimitMinutes())
                    .sectionType(origSec.getSectionType())
                    .navigationMode(origSec.getNavigationMode())
                    .build();
            AssessmentSection savedClonedSec = sectionRepository.save(clonedSec);
            
            // 3. Clone questions mapping
            List<AssessmentQuestion> originalQuestions = questionRepository.findBySectionIdOrderByOrderIndexAsc(origSec.getId());
            for (AssessmentQuestion origQues : originalQuestions) {
                AssessmentQuestion clonedQues = AssessmentQuestion.builder()
                        .assessmentId(savedClone.getId())
                        .sectionId(savedClonedSec.getId())
                        .questionBankId(origQues.getQuestionBankId())
                        .orderIndex(origQues.getOrderIndex())
                        .maxScore(origQues.getMaxScore())
                        .negativeScore(origQues.getNegativeScore())
                        .timeLimitOverride(origQues.getTimeLimitOverride())
                        .build();
                questionRepository.save(clonedQues);
            }
        }
        
        return savedClone;
    }

    @Override
    public Assessment publishAssessment(Long id) {
        Assessment assessment = getAssessmentById(id);
        
        // Validation: Must have at least one section
        List<AssessmentSection> sections = sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(id);
        if (sections.isEmpty()) {
            throw new IllegalStateException("Cannot publish an assessment with no sections");
        }
        
        // Validation: Each section must have at least one question
        for (AssessmentSection section : sections) {
            List<AssessmentQuestion> questions = questionRepository.findBySectionIdOrderByOrderIndexAsc(section.getId());
            if (questions.isEmpty()) {
                throw new IllegalStateException("Section '" + section.getTitle() + "' has no questions mapped");
            }
        }
        
        assessment.setPublished(true);
        return assessmentRepository.save(assessment);
    }

    @Override
    public Assessment unpublishAssessment(Long id) {
        Assessment assessment = getAssessmentById(id);
        assessment.setPublished(false);
        return assessmentRepository.save(assessment);
    }

    @Override
    public AssessmentAssignment assignAssessment(Long id, Long userId, LocalDateTime deadline) {
        // Verify assessment exists
        getAssessmentById(id);
        
        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .assessmentId(id)
                .userId(userId)
                .deadline(deadline)
                .build();
        
        return assignmentRepository.save(assignment);
    }

    @Override
    public List<AssessmentAssignment> getAssignedCandidates(Long id) {
        return assignmentRepository.findByAssessmentId(id);
    }
}
