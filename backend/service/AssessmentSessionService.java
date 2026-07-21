package com.codesphere.backend.service;

import com.codesphere.backend.model.AssessmentAnswer;
import com.codesphere.backend.model.AssessmentSection;
import com.codesphere.backend.model.AssessmentSession;
import com.codesphere.backend.model.Question;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.codesphere.backend.repository.AssessmentSectionRepository;
import com.codesphere.backend.repository.AssessmentSessionRepository;
import com.codesphere.backend.repository.QuestionBankRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssessmentSessionService {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final QuestionBankRepository questionRepository;
    private final McqEvaluationService mcqEvaluationService;
    private final AssessmentSectionRepository sectionRepository;

    public AssessmentSessionService(AssessmentSessionRepository sessionRepository,
                                    AssessmentAnswerRepository answerRepository,
                                    QuestionBankRepository questionRepository,
                                    McqEvaluationService mcqEvaluationService,
                                    AssessmentSectionRepository sectionRepository) {
        this.sessionRepository = sessionRepository;
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.mcqEvaluationService = mcqEvaluationService;
        this.sectionRepository = sectionRepository;
    }

    public AssessmentSession startSession(String username, int durationMinutes) {
        AssessmentSession session = new AssessmentSession();
        session.setUsername(username);
        session.setStartedAt(LocalDateTime.now());
        session.setDurationMinutes(durationMinutes);
        session.setStatus(AssessmentSession.SessionStatus.IN_PROGRESS);
        session.setCurrentSectionIndex(0);
        session.setCurrentSectionStartedAt(LocalDateTime.now());

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

    /**
     * Mixed Assessment Navigation Core Engine Logic
     */
    public List<Question> navigateToSection(Long sessionId, int targetSectionIndex, List<AssessmentSection> allSections, int pageSize) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        int currentIdx = session.getCurrentSectionIndex();

        // 1. Enforce SEQUENTIAL Validation Guard Check
        if (targetSectionIndex > currentIdx) {
            for (int i = currentIdx; i < targetSectionIndex; i++) {
                AssessmentSection previousSection = allSections.get(i);
                if (previousSection.getNavigationMode() == AssessmentSection.NavigationMode.SEQUENTIAL
                        && !session.getCompletedSectionIndexes().contains(i)) {
                    throw new IllegalStateException("Cannot advance. Section " + i + " must be completed first.");
                }
            }
        }

        // 2. Handle per-section isolated countdown timer transitions
        AssessmentSection targetSection = allSections.get(targetSectionIndex);
        LocalDateTime now = LocalDateTime.now();

        if (targetSectionIndex != currentIdx) {
            // Mark previous section complete in tracking sets
            session.getCompletedSectionIndexes().add(currentIdx);
            session.setCurrentSectionIndex(targetSectionIndex);

            if (targetSection.getDurationMinutes() != null) {
                session.setCurrentSectionStartedAt(now);
            } else {
                session.setCurrentSectionStartedAt(null);
            }
            sessionRepository.save(session);
        } else {
            // Evaluate if existing section individual timer has expired
            if (session.getCurrentSectionStartedAt() != null && targetSection.getDurationMinutes() != null) {
                LocalDateTime expirationTime = session.getCurrentSectionStartedAt().plusMinutes(targetSection.getDurationMinutes());
                if (now.isAfter(expirationTime)) {
                    session.getCompletedSectionIndexes().add(currentIdx);
                    if (targetSectionIndex + 1 < allSections.size()) {
                        return navigateToSection(sessionId, targetSectionIndex + 1, allSections, pageSize);
                    } else {
                        submitSession(sessionId);
                        throw new IllegalStateException("Final section runtime expired. Global exam submitted.");
                    }
                }
            }
        }

        return getSectionQuestions(sessionId, targetSectionIndex, pageSize);
    }

    /**
     * Helper mapping method to load sections for structural validation
     */
    public List<AssessmentSection> getAssessmentSectionsForSession(Long sessionId) {
        return sectionRepository.findAll();
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
        int mcqScore = mcqEvaluationService.evaluateSessionMcqs(session);
        System.out.println("Auto-graded MCQ total score for session ID " + session.getId() + ": " + mcqScore);
    }
}