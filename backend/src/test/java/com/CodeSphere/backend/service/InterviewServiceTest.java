package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.interview.*;
import com.CodeSphere.backend.model.Difficulty;
import com.CodeSphere.backend.model.interview.*;
import com.CodeSphere.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @Mock private InterviewCategoryRepository categoryRepository;
    @Mock private InterviewQuestionRepository questionRepository;
    @Mock private InterviewSessionRepository sessionRepository;
    @Mock private InterviewSessionQuestionRepository sessionQuestionRepository;
    @Mock private InterviewAttemptRepository attemptRepository;
    @Mock private InterviewPerformanceRepository performanceRepository;

    @InjectMocks
    private InterviewService interviewService;

    private InterviewCategory category;
    private InterviewQuestion mcqQuestion;
    private InterviewSession session;

    @BeforeEach
    void setUp() {
        category = InterviewCategory.builder().id(1L).name("java").isActive(true).build();

        mcqQuestion = InterviewQuestion.builder()
                .id(100L)
                .category(category)
                .questionType(InterviewQuestionType.MCQ)
                .difficulty(Difficulty.EASY)
                .questionText("What does JVM stand for?")
                .correctAnswer("Java Virtual Machine")
                .build();

        session = InterviewSession.builder()
                .id(1L)
                .userId(10L)
                .sessionType(InterviewSessionType.PRACTICE)
                .status(InterviewSessionStatus.IN_PROGRESS)
                .totalQuestions(1)
                .questionsAnswered(0)
                .correctAnswers(0)
                .score(0)
                .maxScore(10)
                .categoryIds(List.of(1L))
                .build();
    }

    // ---- getCategories ----

    @Test
    void getCategories_FiltersOutInactiveCategories() {
        InterviewCategory inactive = InterviewCategory.builder().id(2L).name("python").isActive(false).build();
        when(categoryRepository.findAll()).thenReturn(List.of(category, inactive));

        List<InterviewCategoryResponse> result = interviewService.getCategories();

        assertEquals(1, result.size());
        assertEquals("java", result.get(0).getName());
    }

    // ---- startSession ----

    @Test
    void startSession_ThrowsException_WhenNoQuestionsAvailable() {
        StartSessionRequest request = new StartSessionRequest();
        request.setCategoryIds(List.of(1L));
        request.setTotalQuestions(5);
        when(questionRepository.findRandomQuestionsByCategories(List.of(1L), 5)).thenReturn(List.of());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> interviewService.startSession(10L, request));

        assertTrue(exception.getMessage().contains("No questions available"));
        verify(sessionRepository, never()).save(any(InterviewSession.class));
    }

    @Test
    void startSession_DefaultsToAllActiveCategories_WhenNoneSpecified() {
        StartSessionRequest request = new StartSessionRequest();
        request.setCategoryIds(null);
        request.setTotalQuestions(1);

        when(categoryRepository.findAll()).thenReturn(List.of(category));
        when(questionRepository.findRandomQuestionsByCategories(List.of(1L), 1)).thenReturn(List.of(mcqQuestion));
        when(sessionRepository.save(any(InterviewSession.class))).thenAnswer(inv -> {
            InterviewSession s = inv.getArgument(0);
            if (s.getId() == null) s.setId(1L);
            return s;
        });

        SessionResultResponse result = interviewService.startSession(10L, request);

        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(1, result.getTotalQuestions());
        verify(sessionQuestionRepository).save(any(InterviewSessionQuestion.class));
    }

    @Test
    void startSession_DefaultsToPracticeType_WhenSessionTypeNotSpecified() {
        StartSessionRequest request = new StartSessionRequest();
        request.setCategoryIds(List.of(1L));
        request.setTotalQuestions(1);
        request.setSessionType(null);

        when(questionRepository.findRandomQuestionsByCategories(List.of(1L), 1)).thenReturn(List.of(mcqQuestion));
        when(sessionRepository.save(any(InterviewSession.class))).thenAnswer(inv -> {
            InterviewSession s = inv.getArgument(0);
            if (s.getId() == null) s.setId(1L);
            return s;
        });

        SessionResultResponse result = interviewService.startSession(10L, request);

        assertEquals("PRACTICE", result.getSessionType());
    }

    // ---- getNextQuestion ----

    @Test
    void getNextQuestion_ThrowsException_WhenSessionNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> interviewService.getNextQuestion(99L, 10L));
    }

    @Test
    void getNextQuestion_ThrowsException_WhenNotSessionOwner() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> interviewService.getNextQuestion(1L, 999L));
    }

    @Test
    void getNextQuestion_ThrowsException_WhenSessionAlreadyCompleted() {
        session.setStatus(InterviewSessionStatus.COMPLETED);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(IllegalStateException.class, () -> interviewService.getNextQuestion(1L, 10L));
    }

    @Test
    void getNextQuestion_ReturnsFirstUnansweredQuestion() {
        InterviewSessionQuestion sq = InterviewSessionQuestion.builder()
                .id(1L).sessionId(1L).question(mcqQuestion).orderIndex(0).isAnswered(false).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(sq));

        InterviewQuestionResponse response = interviewService.getNextQuestion(1L, 10L);

        assertEquals(100L, response.getId());
        assertEquals("MCQ", response.getQuestionType());
    }

    @Test
    void getNextQuestion_ReturnsNull_WhenAllQuestionsAnswered() {
        InterviewSessionQuestion sq = InterviewSessionQuestion.builder()
                .id(1L).sessionId(1L).question(mcqQuestion).orderIndex(0).isAnswered(true).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(sq));

        assertNull(interviewService.getNextQuestion(1L, 10L));
    }

    // ---- submitAnswer ----

    @Test
    void submitAnswer_ThrowsException_WhenNotSessionOwner() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        SubmitAttemptRequest request = new SubmitAttemptRequest();

        assertThrows(IllegalArgumentException.class,
                () -> interviewService.submitAnswer(1L, 100L, 999L, request));
    }

    @Test
    void submitAnswer_ThrowsException_WhenQuestionNotPartOfSession() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        SubmitAttemptRequest request = new SubmitAttemptRequest();

        assertThrows(IllegalArgumentException.class,
                () -> interviewService.submitAnswer(1L, 999L, 10L, request));
    }

    @Test
    void submitAnswer_ThrowsException_WhenQuestionAlreadyAnswered() {
        InterviewSessionQuestion sq = InterviewSessionQuestion.builder()
                .id(1L).sessionId(1L).question(mcqQuestion).orderIndex(0).isAnswered(true).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(sq));
        SubmitAttemptRequest request = new SubmitAttemptRequest();

        assertThrows(IllegalStateException.class,
                () -> interviewService.submitAnswer(1L, 100L, 10L, request));
    }

    @Test
    void submitAnswer_ScoresCorrectMcqAnswer_AndAutoCompletesLastQuestion() {
        InterviewSessionQuestion sq = InterviewSessionQuestion.builder()
                .id(1L).sessionId(1L).question(mcqQuestion).orderIndex(0).isAnswered(false).build();
        SubmitAttemptRequest request = new SubmitAttemptRequest();
        request.setUserAnswer("Java Virtual Machine");
        request.setTimeTakenSeconds(30);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(sq));
        when(performanceRepository.findByUserIdAndCategoryId(10L, 1L)).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        AttemptFeedbackResponse response = interviewService.submitAnswer(1L, 100L, 10L, request);

        assertTrue(response.getIsCorrect());
        assertEquals(10, response.getScore());

        // This is the session's only question (totalQuestions=1), so it must auto-complete
        assertEquals(InterviewSessionStatus.COMPLETED, session.getStatus());
        verify(attemptRepository).save(any(InterviewAttempt.class));
    }

    @Test
    void submitAnswer_ScoresZero_WhenAnswerIsIncorrect() {
        InterviewSessionQuestion sq = InterviewSessionQuestion.builder()
                .id(1L).sessionId(1L).question(mcqQuestion).orderIndex(0).isAnswered(false).build();
        SubmitAttemptRequest request = new SubmitAttemptRequest();
        request.setUserAnswer("Wrong answer");
        request.setTimeTakenSeconds(30);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(sq));
        when(performanceRepository.findByUserIdAndCategoryId(10L, 1L)).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        AttemptFeedbackResponse response = interviewService.submitAnswer(1L, 100L, 10L, request);

        assertFalse(response.getIsCorrect());
        assertEquals(0, response.getScore());
    }

    @Test
    void submitAnswer_AlwaysMarksCodingQuestionsCorrect_InPracticeMode() {
        InterviewQuestion codingQuestion = InterviewQuestion.builder()
                .id(200L).category(category).questionType(InterviewQuestionType.CODING)
                .correctAnswer("N/A").build();
        InterviewSessionQuestion sq = InterviewSessionQuestion.builder()
                .id(1L).sessionId(1L).question(codingQuestion).orderIndex(0).isAnswered(false).build();
        SubmitAttemptRequest request = new SubmitAttemptRequest();
        request.setUserAnswer("some code I wrote");
        request.setTimeTakenSeconds(120);

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(sq));
        when(performanceRepository.findByUserIdAndCategoryId(10L, 1L)).thenReturn(Optional.empty());
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        AttemptFeedbackResponse response = interviewService.submitAnswer(1L, 200L, 10L, request);

        assertTrue(response.getIsCorrect(), "Coding questions are auto-accepted in practice mode pending manual/judge grading");
    }

    // ---- completeSession ----

    @Test
    void completeSession_ThrowsException_WhenNotSessionOwner() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        assertThrows(IllegalArgumentException.class, () -> interviewService.completeSession(1L, 999L));
    }

    @Test
    void completeSession_IsIdempotent_WhenAlreadyCompleted() {
        session.setStatus(InterviewSessionStatus.COMPLETED);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        when(sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());
        when(attemptRepository.findBySessionId(1L)).thenReturn(List.of());

        SessionResultResponse result = interviewService.completeSession(1L, 10L);

        assertEquals("COMPLETED", result.getStatus());
        // Must not attempt to update per-category performance a second time
        verify(performanceRepository, never()).findByUserIdAndCategoryId(any(), any());
    }
}
