package com.codesphere.backend.service;

import com.codesphere.backend.model.AssessmentAnswer;
import com.codesphere.backend.model.AssessmentSession;
import com.codesphere.backend.model.Question;
import com.codesphere.backend.repository.AssessmentAnswerRepository;
import com.codesphere.backend.repository.AssessmentSessionRepository;
import com.codesphere.backend.repository.QuestionBankRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssessmentSessionService {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final QuestionBankRepository questionRepository;
    private final McqEvaluationService mcqEvaluationService;

    public AssessmentSessionService(AssessmentSessionRepository sessionRepository,
                                    AssessmentAnswerRepository answerRepository,
                                    QuestionBankRepository questionRepository,
                                    McqEvaluationService mcqEvaluationService) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.mcqEvaluationService = mcqEvaluationService;
    }

    public AssessmentSession startSession(String username, int durationMinutes) {
        AssessmentSession session = new AssessmentSession();
        session.setUsername(username);
        session.setStartedAt(LocalDateTime.now());
        session.setDurationMinutes(durationMinutes);
        session.setStatus(AssessmentSession.SessionStatus.IN_PROGRESS);

        // Snapshot all compiled questions in a randomized sequence order
        List<Long> questionIds = questionRepository.findAll().stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        Collections.shuffle(questionIds);
        session.setQuestionIdsSnapshot(questionIds);

        return sessionRepository.save(session);
    }

    public AssessmentAnswer autoSaveAnswer(Long sessionId, Long questionId, String answerContent) {
        AssessmentSession session = verifyActiveSession(sessionId);

        AssessmentAnswer answer = answerRepository.findBySessionIdAndQuestionId(sessionId, questionId)
                .orElse(new AssessmentAnswer());

        answer.setSessionId(sessionId);
        answer.setQuestionId(questionId);
        answer.setSelectedAnswer(answerContent);
        answer.setUpdatedAt(LocalDateTime.now());

        return answerRepository.save(answer);
    }

    public AssessmentSession resumeSession(Long sessionId) {
        return verifyActiveSession(sessionId);
    }

    public AssessmentSession submitSession(Long sessionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is already closed");
        }

        session.setStatus(AssessmentSession.SessionStatus.SUBMITTED);
        session.setSubmittedAt(LocalDateTime.now());

        AssessmentSession savedSession = sessionRepository.save(session);
        triggerEvaluationPipeline(savedSession);
        return savedSession;
    }

    public List<Question> getSectionQuestions(Long sessionId, int sectionIndex, int pageSize) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        List<Long> snapshot = session.getQuestionIdsSnapshot();
        int fromIndex = sectionIndex * pageSize;

        if (fromIndex >= snapshot.size() || fromIndex < 0) {
            return List.of();
        }

        int toIndex = Math.min(fromIndex + pageSize, snapshot.size());
        List<Long> sectionIds = snapshot.subList(fromIndex, toIndex);

        return questionRepository.findAllById(sectionIds);
    }

    // Server-side active validation sweeps firing every 10 seconds
    @Scheduled(fixedRate = 10000)
    public void enforceExpirationTimers() {
        List<AssessmentSession> activeSessions = sessionRepository.findByStatus(AssessmentSession.SessionStatus.IN_PROGRESS);
        LocalDateTime now = LocalDateTime.now();

        for (AssessmentSession session : activeSessions) {
            if (session.getStartedAt().plusMinutes(session.getDurationMinutes()).isBefore(now)) {
                session.setStatus(AssessmentSession.SessionStatus.TIMED_OUT);
                session.setSubmittedAt(now);
                sessionRepository.save(session);
                triggerEvaluationPipeline(session);
            }
        }
    }

    private AssessmentSession verifyActiveSession(Long sessionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is no longer active");
        }

        if (session.getStartedAt().plusMinutes(session.getDurationMinutes()).isBefore(LocalDateTime.now())) {
            session.setStatus(AssessmentSession.SessionStatus.TIMED_OUT);
            session.setSubmittedAt(LocalDateTime.now());
            sessionRepository.save(session);
            throw new IllegalStateException("Session time limit has already elapsed");
        }

        return session;
    }

    private void triggerEvaluationPipeline(AssessmentSession session) {
        // Runs the auto-grading pipeline mechanics for the completed attempt
        int mcqScore = mcqEvaluationService.evaluateSessionMcqs(session);
        System.out.println("Auto-graded MCQ total score for session ID " + session.getId() + ": " + mcqScore);
    }
}