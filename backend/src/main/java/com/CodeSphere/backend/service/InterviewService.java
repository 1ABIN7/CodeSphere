package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.interview.*;
import com.CodeSphere.backend.model.interview.*;
import com.CodeSphere.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private final InterviewCategoryRepository categoryRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewSessionQuestionRepository sessionQuestionRepository;
    private final InterviewAttemptRepository attemptRepository;
    private final InterviewPerformanceRepository performanceRepository;

    public List<InterviewCategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .filter(InterviewCategory::isActive)
                .map(cat -> InterviewCategoryResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .displayName(cat.getDisplayName())
                        .description(cat.getDescription())
                        .icon(cat.getIcon())
                        .color(cat.getColor())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public SessionResultResponse startSession(Long userId, StartSessionRequest request) {
        log.info("Starting interview session for user {} with categories {}", userId, request.getCategoryIds());

        // Default to all categories if none provided
        List<Long> categories = request.getCategoryIds();
        if (categories == null || categories.isEmpty()) {
            categories = categoryRepository.findAll().stream()
                    .filter(InterviewCategory::isActive)
                    .map(InterviewCategory::getId)
                    .collect(Collectors.toList());
        }

        int limit = request.getTotalQuestions() != null ? request.getTotalQuestions() : 10;
        List<InterviewQuestion> randomQuestions = questionRepository.findRandomQuestionsByCategories(categories, limit);

        if (randomQuestions.isEmpty()) {
            throw new IllegalStateException("No questions available for the selected categories.");
        }

        InterviewSession session = InterviewSession.builder()
                .userId(userId)
                .sessionType(InterviewSessionType.valueOf(request.getSessionType() != null ? request.getSessionType() : "PRACTICE"))
                .categoryIds(categories)
                .totalQuestions(randomQuestions.size())
                .status(InterviewSessionStatus.IN_PROGRESS)
                .build();
        session = sessionRepository.save(session);

        int maxScore = 0;
        for (int i = 0; i < randomQuestions.size(); i++) {
            InterviewQuestion q = randomQuestions.get(i);
            maxScore += 10; // Assume 10 points per question for now

            InterviewSessionQuestion sq = InterviewSessionQuestion.builder()
                    .sessionId(session.getId())
                    .question(q)
                    .orderIndex(i)
                    .build();
            sessionQuestionRepository.save(sq);
        }

        session.setMaxScore(maxScore);
        sessionRepository.save(session);

        return SessionResultResponse.builder()
                .sessionId(session.getId())
                .sessionType(session.getSessionType().name())
                .status(session.getStatus().name())
                .totalQuestions(session.getTotalQuestions())
                .build();
    }

    @Transactional(readOnly = true)
    public InterviewQuestionResponse getNextQuestion(Long sessionId, Long userId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to access this session");
        }

        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is not in progress");
        }

        List<InterviewSessionQuestion> sqs = sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
        for (InterviewSessionQuestion sq : sqs) {
            if (!sq.isAnswered()) {
                InterviewQuestion q = sq.getQuestion();
                return InterviewQuestionResponse.builder()
                        .id(q.getId())
                        .categoryId(q.getCategory().getId())
                        .questionType(q.getQuestionType().name())
                        .difficulty(q.getDifficulty().name())
                        .topic(q.getTopic())
                        .questionText(q.getQuestionText())
                        .options(q.getOptions())
                        .timeLimitSeconds(q.getTimeLimitSeconds())
                        .build();
            }
        }
        return null; // All questions answered
    }

    @Transactional
    public AttemptFeedbackResponse submitAnswer(Long sessionId, Long questionId, Long userId, SubmitAttemptRequest request) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        if (session.getStatus() != InterviewSessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("Session is not in progress");
        }

        InterviewSessionQuestion sq = sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId)
                .stream()
                .filter(s -> s.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not part of this session"));

        if (sq.isAnswered()) {
            throw new IllegalStateException("Question already answered");
        }

        InterviewQuestion q = sq.getQuestion();
        boolean isCorrect = evaluateAnswer(q, request.getUserAnswer());
        int score = isCorrect ? 10 : 0;

        InterviewAttempt attempt = InterviewAttempt.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .userAnswer(request.getUserAnswer())
                .isCorrect(isCorrect)
                .timeTakenSeconds(request.getTimeTakenSeconds())
                .score(score)
                .build();
        attemptRepository.save(attempt);

        sq.setAnswered(true);
        sessionQuestionRepository.save(sq);

        session.setQuestionsAnswered(session.getQuestionsAnswered() + 1);
        if (isCorrect) {
            session.setCorrectAnswers(session.getCorrectAnswers() + 1);
            session.setScore(session.getScore() + score);
        }
        
        // Auto-complete if all answered
        if (session.getQuestionsAnswered().equals(session.getTotalQuestions())) {
            completeSessionInternal(session);
        } else {
            sessionRepository.save(session);
        }

        updatePerformance(userId, q.getCategory().getId(), isCorrect, request.getTimeTakenSeconds(), score);

        return AttemptFeedbackResponse.builder()
                .isCorrect(isCorrect)
                .correctAnswer(q.getCorrectAnswer())
                .explanation(q.getExplanation())
                .score(score)
                .build();
    }

    @Transactional
    public SessionResultResponse completeSession(Long sessionId, Long userId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        
        if (session.getStatus() == InterviewSessionStatus.IN_PROGRESS) {
            completeSessionInternal(session);
        }
        
        return buildSessionResult(session);
    }
    
    private void completeSessionInternal(InterviewSession session) {
        session.setStatus(InterviewSessionStatus.COMPLETED);
        session.setCompletedAt(OffsetDateTime.now());
        sessionRepository.save(session);
        
        // Update total sessions in performance for each category involved
        for (Long catId : session.getCategoryIds()) {
            InterviewPerformance perf = performanceRepository.findByUserIdAndCategoryId(session.getUserId(), catId)
                .orElse(InterviewPerformance.builder().userId(session.getUserId())
                    .category(categoryRepository.findById(catId).orElse(null)).build());
            
            perf.setTotalSessions(perf.getTotalSessions() + 1);
            performanceRepository.save(perf);
        }
    }

    private boolean evaluateAnswer(InterviewQuestion q, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return false;
        }
        
        switch (q.getQuestionType()) {
            case MCQ:
            case TRUE_FALSE:
                return q.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim());
            case FILL_BLANK:
            case SHORT_ANSWER:
                // Simple string match for now; could be regex or NLP-based in the future
                return q.getCorrectAnswer().equalsIgnoreCase(userAnswer.trim());
            case CODING:
            case CASE_STUDY:
                // These usually require manual grading or separate judge evaluation. 
                // We'll mark them correct if they submitted anything for now in practice mode.
                return true; 
            default:
                return false;
        }
    }

    private void updatePerformance(Long userId, Long categoryId, boolean isCorrect, int timeTaken, int score) {
        InterviewCategory category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) return;

        InterviewPerformance perf = performanceRepository.findByUserIdAndCategoryId(userId, categoryId)
                .orElse(InterviewPerformance.builder()
                        .userId(userId)
                        .category(category)
                        .build());

        perf.setTotalAttempted(perf.getTotalAttempted() + 1);
        if (isCorrect) {
            perf.setCorrectCount(perf.getCorrectCount() + 1);
        }
        if (score > perf.getBestScore()) {
            perf.setBestScore(score);
        }
        
        // Update running average time
        double totalTime = (perf.getAvgTimeSeconds() * (perf.getTotalAttempted() - 1)) + timeTaken;
        perf.setAvgTimeSeconds(totalTime / perf.getTotalAttempted());
        
        perf.setLastAttemptedAt(OffsetDateTime.now());
        performanceRepository.save(perf);
    }
    
    public SessionResultResponse getSessionResult(Long sessionId, Long userId) {
        InterviewSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("Session not found"));
        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        return buildSessionResult(session);
    }
    
    private SessionResultResponse buildSessionResult(InterviewSession session) {
        List<SessionResultResponse.AttemptDetail> attemptDetails = new ArrayList<>();
        
        List<InterviewSessionQuestion> sqs = sessionQuestionRepository.findBySessionIdOrderByOrderIndexAsc(session.getId());
        List<InterviewAttempt> attempts = attemptRepository.findBySessionId(session.getId());
        
        for (InterviewSessionQuestion sq : sqs) {
            InterviewQuestion q = sq.getQuestion();
            InterviewAttempt attempt = attempts.stream()
                    .filter(a -> a.getQuestionId().equals(q.getId()))
                    .findFirst().orElse(null);
                    
            SessionResultResponse.AttemptDetail detail = SessionResultResponse.AttemptDetail.builder()
                    .questionId(q.getId())
                    .questionText(q.getQuestionText())
                    .questionType(q.getQuestionType().name())
                    .build();
                    
            if (attempt != null) {
                detail.setUserAnswer(attempt.getUserAnswer());
                detail.setCorrectAnswer(q.getCorrectAnswer());
                detail.setExplanation(q.getExplanation());
                detail.setIsCorrect(attempt.isCorrect());
                detail.setTimeTakenSeconds(attempt.getTimeTakenSeconds());
                detail.setScore(attempt.getScore());
            }
            
            attemptDetails.add(detail);
        }
        
        return SessionResultResponse.builder()
                .sessionId(session.getId())
                .sessionType(session.getSessionType().name())
                .status(session.getStatus().name())
                .score(session.getScore())
                .maxScore(session.getMaxScore())
                .questionsAnswered(session.getQuestionsAnswered())
                .totalQuestions(session.getTotalQuestions())
                .correctAnswers(session.getCorrectAnswers())
                .attempts(attemptDetails)
                .build();
    }

    public PerformanceSummaryResponse getPerformanceSummary(Long userId) {
        List<InterviewPerformance> perfs = performanceRepository.findByUserId(userId);
        
        int totalQuestionsAttempted = 0;
        int totalCorrect = 0;
        int maxTotalSessions = 0;
        
        List<CategoryStatsResponse> catStats = new ArrayList<>();
        
        for (InterviewPerformance p : perfs) {
            totalQuestionsAttempted += p.getTotalAttempted();
            totalCorrect += p.getCorrectCount();
            if (p.getTotalSessions() > maxTotalSessions) {
                maxTotalSessions = p.getTotalSessions();
            }
            
            double accuracy = p.getTotalAttempted() > 0 ? (double) p.getCorrectCount() / p.getTotalAttempted() * 100 : 0.0;
            
            catStats.add(CategoryStatsResponse.builder()
                    .categoryId(p.getCategory().getId())
                    .categoryName(p.getCategory().getName())
                    .categoryDisplayName(p.getCategory().getDisplayName())
                    .totalAttempted(p.getTotalAttempted())
                    .correctCount(p.getCorrectCount())
                    .accuracy(accuracy)
                    .bestScore(p.getBestScore())
                    .avgTimeSeconds(p.getAvgTimeSeconds())
                    .build());
        }
        
        double overallAccuracy = totalQuestionsAttempted > 0 ? (double) totalCorrect / totalQuestionsAttempted * 100 : 0.0;
        
        return PerformanceSummaryResponse.builder()
                .userId(userId)
                .totalSessionsCompleted(maxTotalSessions) // rough estimate
                .totalQuestionsAttempted(totalQuestionsAttempted)
                .overallAccuracy(overallAccuracy)
                .categoryStats(catStats)
                .build();
    }
}
