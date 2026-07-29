package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import com.CodeSphere.backend.entity.AssessmentQuestion;
import com.CodeSphere.backend.model.AssessmentSection;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.AssessmentAssignmentRepository;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSectionRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.McqEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentServiceImplTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentSectionRepository sectionRepository;
    @Mock private AssessmentQuestionRepository questionRepository;
    @Mock private AssessmentAssignmentRepository assignmentRepository;
    @Mock private AssessmentSessionRepository assessmentSessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private AssessmentAnswerRepository answerRepository;
    @Mock private QuestionBankRepository questionBankRepository;
    @Mock private McqEvaluationService mcqEvaluationService;

    @InjectMocks
    private AssessmentServiceImpl assessmentService;

    private Assessment assessment;

    @BeforeEach
    void setUp() {
        assessment = Assessment.builder()
                .id(1L)
                .title("Java Fundamentals")
                .assessmentType("MCQ")
                .durationMinutes(60)
                .isPublished(false)
                .build();
    }

    // ---- createAssessment ----

    @Test
    void createAssessment_AlwaysCreatesAsUnpublished() {
        Assessment incoming = Assessment.builder().title("New Test").isPublished(true).build();
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        Assessment result = assessmentService.createAssessment(incoming);

        assertFalse(result.isPublished(), "New assessments must always start unpublished regardless of input");
    }

    // ---- deleteAssessment ----

    @Test
    void deleteAssessment_Success_CascadesQuestionsAndSectionsFirst() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));

        assessmentService.deleteAssessment(1L);

        verify(questionRepository).deleteByAssessmentId(1L);
        verify(sectionRepository).deleteByAssessmentId(1L);
        verify(assessmentRepository).delete(assessment);
    }

    @Test
    void deleteAssessment_ThrowsException_WhenNotFound() {
        when(assessmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> assessmentService.deleteAssessment(99L));
        verify(assessmentRepository, never()).delete(any());
    }

    // ---- publishAssessment: the validation that matters most ----

    @Test
    void publishAssessment_ThrowsException_WhenNoSections() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(1L)).thenReturn(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> assessmentService.publishAssessment(1L));

        assertTrue(exception.getMessage().contains("no sections"));
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void publishAssessment_ThrowsException_WhenASectionHasNoQuestions() {
        AssessmentSection emptySection = AssessmentSection.builder().id(10L).title("Section A").build();
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(1L)).thenReturn(List.of(emptySection));
        when(questionRepository.findBySectionIdOrderByOrderIndexAsc(10L)).thenReturn(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> assessmentService.publishAssessment(1L));

        assertTrue(exception.getMessage().contains("Section A"));
        assertTrue(exception.getMessage().contains("no questions"));
        verify(assessmentRepository, never()).save(any(Assessment.class));
    }

    @Test
    void publishAssessment_Success_WhenEverySectionHasQuestions() {
        AssessmentSection section = AssessmentSection.builder().id(10L).title("Section A").build();
        AssessmentQuestion question = AssessmentQuestion.builder().id(100L).sectionId(10L).build();

        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(1L)).thenReturn(List.of(section));
        when(questionRepository.findBySectionIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(question));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        Assessment result = assessmentService.publishAssessment(1L);

        assertTrue(result.isPublished());
    }

    @Test
    void unpublishAssessment_Success_DoesNotRunPublishValidation() {
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(inv -> inv.getArgument(0));

        Assessment result = assessmentService.unpublishAssessment(1L);

        assertFalse(result.isPublished());
        verify(sectionRepository, never()).findByAssessmentIdOrderBySectionOrderAsc(anyLong());
    }

    // ---- cloneAssessment: deep clone ----

    @Test
    void cloneAssessment_CopiesAssessmentSectionsAndQuestions_AsUnpublished() {
        AssessmentSection origSection = AssessmentSection.builder()
                .id(10L).assessmentId(1L).title("Section A").sectionOrder(1).build();
        AssessmentQuestion origQuestion = AssessmentQuestion.builder()
                .id(100L).assessmentId(1L).sectionId(10L).questionBankId(500L).orderIndex(1).build();

        Assessment clonedShell = Assessment.builder().id(2L).title("Java Fundamentals - Clone").build();
        AssessmentSection clonedSection = AssessmentSection.builder().id(20L).assessmentId(2L).build();

        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(assessmentRepository.save(any(Assessment.class))).thenReturn(clonedShell);
        when(sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(1L)).thenReturn(List.of(origSection));
        when(sectionRepository.save(any(AssessmentSection.class))).thenReturn(clonedSection);
        when(questionRepository.findBySectionIdOrderByOrderIndexAsc(10L)).thenReturn(List.of(origQuestion));

        Assessment clone = assessmentService.cloneAssessment(1L);

        assertEquals("Java Fundamentals - Clone", clone.getTitle());

        ArgumentCaptor<Assessment> assessmentCaptor = ArgumentCaptor.forClass(Assessment.class);
        verify(assessmentRepository).save(assessmentCaptor.capture());
        assertFalse(assessmentCaptor.getValue().isPublished(), "Clones must never inherit published state");

        verify(sectionRepository).save(any(AssessmentSection.class));
        verify(questionRepository).save(any(AssessmentQuestion.class));
    }

    @Test
    void cloneAssessment_ThrowsException_WhenOriginalNotFound() {
        when(assessmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> assessmentService.cloneAssessment(99L));
        verify(sectionRepository, never()).findByAssessmentIdOrderBySectionOrderAsc(anyLong());
    }

    // ---- assignAssessment: candidate assignment ----

    @Test
    void assignAssessment_Success_CreatesAssignmentWithDeadline() {
        LocalDateTime deadline = LocalDateTime.now().plusDays(7);
        when(assessmentRepository.findById(1L)).thenReturn(Optional.of(assessment));
        when(assignmentRepository.save(any(AssessmentAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

        AssessmentAssignment result = assessmentService.assignAssessment(1L, 55L, deadline);

        assertEquals(1L, result.getAssessmentId());
        assertEquals(55L, result.getUserId());
        assertEquals(deadline, result.getDeadline());
    }

    @Test
    void assignAssessment_ThrowsException_WhenAssessmentNotFound() {
        when(assessmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> assessmentService.assignAssessment(99L, 55L, LocalDateTime.now()));
        verify(assignmentRepository, never()).save(any(AssessmentAssignment.class));
    }

    @Test
    void getAssignedCandidates_ReturnsAllAssignmentsForAssessment() {
        AssessmentAssignment a1 = AssessmentAssignment.builder().id(1L).assessmentId(1L).userId(10L).build();
        AssessmentAssignment a2 = AssessmentAssignment.builder().id(2L).assessmentId(1L).userId(20L).build();
        when(assignmentRepository.findByAssessmentId(1L)).thenReturn(List.of(a1, a2));

        List<AssessmentAssignment> result = assessmentService.getAssignedCandidates(1L);

        assertEquals(2, result.size());
    }
}
