package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AssessmentSection;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.AssessmentSectionRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.impl.AssessmentSessionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssessmentSessionServiceImplTest {

    @Mock private AssessmentSessionRepository sessionRepository;
    @Mock private AssessmentAnswerRepository answerRepository;
    @Mock private QuestionBankRepository questionRepository;
    @Mock private McqEvaluationService mcqEvaluationService;
    @Mock private AssessmentSectionRepository sectionRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private AssessmentSessionServiceImpl sessionService;

    @Test
    void expiredSectionMovesToTheNextSectionOnTheServer() {
        AssessmentSession session = activeSession(0, OffsetDateTime.now().minusMinutes(2));
        List<AssessmentSection> sections = List.of(section(1L, 1), section(2L, 5));
        when(sessionRepository.findById(9L)).thenReturn(Optional.of(session));
        when(sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(42L)).thenReturn(sections);
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssessmentSession result = sessionService.navigateToSection(9L, 1);

        assertEquals(1, result.getCurrentSectionIndex());
        assertTrue(result.getCompletedSectionIndexes().contains(0));
        assertNotNull(result.getCurrentSectionStartedAt());
    }

    @Test
    void expiredFinalSectionEndsTheSessionInsteadOfTrustingTheBrowser() {
        AssessmentSession session = activeSession(0, OffsetDateTime.now().minusMinutes(2));
        when(sessionRepository.findById(9L)).thenReturn(Optional.of(session));
        when(sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(42L)).thenReturn(List.of(section(1L, 1)));
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> sessionService.navigateToSection(9L, 0));

        assertEquals("The assessment time has expired", exception.getMessage());
        assertEquals(AssessmentSession.SessionStatus.TIMED_OUT, session.getStatus());
        assertNotNull(session.getSubmittedAt());
    }

    @Test
    void movingForwardStartsTheNextSectionDeadlineOnTheServer() {
        AssessmentSession session = activeSession(0, OffsetDateTime.now());
        List<AssessmentSection> sections = List.of(section(1L, 10), section(2L, 10));
        when(sessionRepository.findById(9L)).thenReturn(Optional.of(session));
        when(sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(42L)).thenReturn(sections);
        when(sessionRepository.save(any(AssessmentSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sessionService.navigateToSection(9L, 1);

        assertEquals(1, session.getCurrentSectionIndex());
        assertTrue(session.getCompletedSectionIndexes().contains(0));
    }

    private AssessmentSession activeSession(int index, OffsetDateTime sectionStartedAt) {
        return AssessmentSession.builder()
                .id(9L).assessmentId(42L).durationMinutes(60)
                .startedAt(OffsetDateTime.now().minusMinutes(3))
                .currentSectionIndex(index).currentSectionStartedAt(sectionStartedAt)
                .status(AssessmentSession.SessionStatus.IN_PROGRESS).build();
    }

    private AssessmentSection section(long id, int minutes) {
        return AssessmentSection.builder().id(id).assessmentId(42L)
                .durationMinutes(minutes).navigationMode(AssessmentSection.NavigationMode.FREE).build();
    }
}
