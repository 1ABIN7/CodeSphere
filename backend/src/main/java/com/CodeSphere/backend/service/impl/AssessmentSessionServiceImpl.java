package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.AssessmentAnswer;
//import com.CodeSphere.backend.model.AssessmentSection;
import com.CodeSphere.backend.model.AssessmentSection;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.AssessmentSectionRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.service.AssessmentSessionService;
import com.CodeSphere.backend.service.McqEvaluationService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AssessmentSessionServiceImpl implements AssessmentSessionService {

    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final QuestionBankRepository questionRepository;
    private final McqEvaluationService mcqEvaluationService;
    private final AssessmentSectionRepository sectionRepository;
    private final UserRepository userRepository;

    @Override
    public AssessmentSession startSession(String username, int durationMinutes) {
        User candidate = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found with username: " + username));

        AssessmentSession session = new AssessmentSession();
        session.setCandidate(candidate);
        session.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC));
        session.setDurationMinutes(durationMinutes);
        session.setStatus(AssessmentSession.SessionStatus.IN_PROGRESS);
        session.setCurrentSectionIndex(0);
        session.setCurrentSectionStartedAt(OffsetDateTime.now(ZoneOffset.UTC));

        // Snapshot all compiled questions in a randomized sequence order
        List<Long> questionIds = questionRepository.findAll().stream()
                .map(Question::getId)
                .collect(Collectors.toList());
        Collections.shuffle(questionIds);
        session.setQuestionIdsSnapshot(questionIds);

        return sessionRepository.save(session);
    }

    @Override
    public AssessmentAnswer autoSaveAnswer(Long sessionId, Long questionId, String answerContent) {
        verifyActiveSession(sessionId);

        AssessmentAnswer answer = answerRepository.findBySessionIdAndQuestionId(sessionId, questionId)
                .orElse(new AssessmentAnswer());

        answer.setSessionId(sessionId);
        answer.setQuestionId(questionId);
        answer.setSelectedAnswer(answerContent);
        answer.setUpdatedAt(LocalDateTime.now());

        return answerRepository.save(answer);
    }

    @Override
    public AssessmentSession resumeSession(Long sessionId) {
        return verifyActiveSession(sessionId);
    }

    @Override
    public AssessmentSession submitSession(Long sessionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is already closed");
        }

        session.setStatus(AssessmentSession.SessionStatus.SUBMITTED);
        session.setSubmittedAt(OffsetDateTime.now(ZoneOffset.UTC));

        AssessmentSession savedSession = sessionRepository.save(session);
        triggerEvaluationPipeline(savedSession);
        return savedSession;
    }

    @Override
    @Transactional(readOnly = true)
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

    @Override
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
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (targetSectionIndex != currentIdx) {
            session.getCompletedSectionIndexes().add(currentIdx);
            session.setCurrentSectionIndex(targetSectionIndex);

            if (targetSection.getDurationMinutes() != null) {
                session.setCurrentSectionStartedAt(now);
            } else {
                session.setCurrentSectionStartedAt(null);
            }
            sessionRepository.save(session);
        } else {
            if (session.getCurrentSectionStartedAt() != null && targetSection.getDurationMinutes() != null) {
                OffsetDateTime expirationTime = session.getCurrentSectionStartedAt().plusMinutes(targetSection.getDurationMinutes());
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

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentSection> getAssessmentSectionsForSession(Long sessionId) {
        return sectionRepository.findAll();
    }

    @Override
    @Scheduled(fixedRate = 10000)
    public void enforceExpirationTimers() {
        List<AssessmentSession> activeSessions = sessionRepository.findByStatus(AssessmentSession.SessionStatus.IN_PROGRESS);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (AssessmentSession session : activeSessions) {
            if (session.getStartedAt().plusMinutes(session.getDurationMinutes()).isBefore(now)) {
                session.setStatus(AssessmentSession.SessionStatus.TIMED_OUT);
                session.setSubmittedAt(now);
                sessionRepository.save(session);
                triggerEvaluationPipeline(session);
            }
        }
    }

    // Helper Private Methods

    private AssessmentSession verifyActiveSession(Long sessionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is no longer active");
        }

        if (session.getStartedAt().plusMinutes(session.getDurationMinutes()).isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            session.setStatus(AssessmentSession.SessionStatus.TIMED_OUT);
            session.setSubmittedAt(OffsetDateTime.now(ZoneOffset.UTC));
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