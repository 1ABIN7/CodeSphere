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
        AssessmentSession session = verifyActiveSession(sessionId);
        enforceCurrentSectionDeadline(session,
                sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(session.getAssessmentId()),
                OffsetDateTime.now(ZoneOffset.UTC));
        if (session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("The assessment time has expired");
        }

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
    public AssessmentSession navigateToSection(Long sessionId, int targetSectionIndex) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        if (session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is no longer active");
        }
        List<AssessmentSection> allSections = sectionRepository
                .findByAssessmentIdOrderBySectionOrderAsc(session.getAssessmentId());
        if (targetSectionIndex < 0 || targetSectionIndex >= allSections.size()) {
            throw new IllegalArgumentException("Section does not exist");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        enforceCurrentSectionDeadline(session, allSections, now);
        if (session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("The assessment time has expired");
        }

        int currentIdx = session.getCurrentSectionIndex();
        if (targetSectionIndex < currentIdx) {
            throw new IllegalStateException("A completed section cannot be reopened.");
        }
        if (targetSectionIndex > currentIdx + 1) {
            throw new IllegalStateException("Sections must be completed in order.");
        }
        if (targetSectionIndex == currentIdx) return session;

        session.getCompletedSectionIndexes().add(currentIdx);
        session.setCurrentSectionIndex(targetSectionIndex);
        session.setCurrentSectionStartedAt(now);
        return sessionRepository.save(session);
    }

    private void enforceCurrentSectionDeadline(AssessmentSession session, List<AssessmentSection> sections, OffsetDateTime now) {
        if (sections.isEmpty() || session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) return;
        int index = session.getCurrentSectionIndex();
        if (index < 0 || index >= sections.size()) {
            throw new IllegalStateException("The session has an invalid current section");
        }
        AssessmentSection current = sections.get(index);
        if (current.getDurationMinutes() == null || current.getDurationMinutes() <= 0) return;
        if (session.getCurrentSectionStartedAt() == null) {
            session.setCurrentSectionStartedAt(now);
            sessionRepository.save(session);
            return;
        }
        if (now.isBefore(session.getCurrentSectionStartedAt().plusMinutes(current.getDurationMinutes()))) return;

        session.getCompletedSectionIndexes().add(index);
        if (index + 1 < sections.size()) {
            session.setCurrentSectionIndex(index + 1);
            session.setCurrentSectionStartedAt(now);
            sessionRepository.save(session);
            return;
        }
        session.setStatus(AssessmentSession.SessionStatus.TIMED_OUT);
        session.setSubmittedAt(now);
        sessionRepository.save(session);
        triggerEvaluationPipeline(session);
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
                continue;
            }
            enforceCurrentSectionDeadline(session,
                    sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(session.getAssessmentId()), now);
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
