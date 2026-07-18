package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.entity.AssessmentQuestion;
import com.CodeSphere.backend.model.*;
import com.CodeSphere.backend.repository.*;
import com.CodeSphere.backend.service.CertificationService;
import com.CodeSphere.backend.service.EvaluationQueueService;
import com.CodeSphere.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationQueueServiceImpl implements EvaluationQueueService {

    private final SessionAnswerRepository answerRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final EvaluationReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AssessmentRepository assessmentRepository;
    private final CertificationService certificationService;

    @Override
    public Page<SessionAnswer> getPendingEvaluations(Long evaluatorId, int page, int size) {
        return answerRepository.findByEvaluatedByAndScoreIsNull(evaluatorId, PageRequest.of(page, size));
    }

    @Override
    @Transactional
    public void assignPendingAnswer(Long answerId) {
        SessionAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found: " + answerId));

        // Find all available evaluators (Role other than CANDIDATE)
        List<User> evaluators = userRepository.findAll().stream()
                .filter(u -> u.getRole() != Role.ROLE_CANDIDATE)
                .collect(Collectors.toList());

        if (evaluators.isEmpty()) {
            log.warn("No available evaluators found in system to assign answer #{}", answerId);
            return;
        }

        // Find the evaluator with the least pending assignments
        User selectedEvaluator = null;
        long minPendingCount = Long.MAX_VALUE;

        for (User evaluator : evaluators) {
            long pendingCount = answerRepository.countPendingEvaluationsByEvaluatorId(evaluator.getId());
            if (pendingCount < minPendingCount) {
                minPendingCount = pendingCount;
                selectedEvaluator = evaluator;
            }
        }

        if (selectedEvaluator != null) {
            answer.setEvaluatedBy(selectedEvaluator.getId());
            answerRepository.save(answer);
            log.info("Assigned answer #{} to evaluator {} (pending workload: {})",
                    answerId, selectedEvaluator.getUsername(), minPendingCount);

            // Notify evaluator
            notificationService.notifyEvaluator(selectedEvaluator.getId(), answerId);
        }
    }

    @Override
    @Transactional
    public void submitScore(Long answerId, Double score, String feedback, Long evaluatorId) {
        // Save the review
        EvaluationReview review = EvaluationReview.builder()
                .answerId(answerId)
                .evaluatorId(evaluatorId)
                .scores(Map.of("score", score))
                .feedback(feedback)
                .build();
        reviewRepository.save(review);

        aggregateReviewScores(answerId);
    }

    @Override
    @Transactional
    public void submitRubricScores(Long answerId, Map<String, Object> rubricScores, String feedback, Long evaluatorId) {
        // Calculate total score from rubric values
        double totalScore = 0.0;
        for (Object value : rubricScores.values()) {
            if (value instanceof Number) {
                totalScore += ((Number) value).doubleValue();
            }
        }

        Map<String, Object> scoresMap = new HashMap<>(rubricScores);
        scoresMap.put("score", totalScore);

        EvaluationReview review = EvaluationReview.builder()
                .answerId(answerId)
                .evaluatorId(evaluatorId)
                .scores(scoresMap)
                .feedback(feedback)
                .build();
        reviewRepository.save(review);

        aggregateReviewScores(answerId);
    }

    @Override
    public List<EvaluationReview> getReviewsForAnswer(Long answerId) {
        return reviewRepository.findByAnswerId(answerId);
    }

    private void aggregateReviewScores(Long answerId) {
        SessionAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found: " + answerId));
        AssessmentSession session = sessionRepository.findById(answer.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + answer.getSessionId()));

        List<EvaluationReview> reviews = reviewRepository.findByAnswerId(answerId);
        if (reviews.isEmpty()) return;

        // Fetch question max score mapping
        List<AssessmentQuestion> mappings = assessmentQuestionRepository.findByAssessmentId(session.getAssessmentId());
        double maxQuestionScore = mappings.stream()
                .filter(m -> m.getQuestionBankId().equals(answer.getQuestionId()))
                .map(AssessmentQuestion::getMaxScore)
                .findFirst()
                .orElse(100.0);

        if (reviews.size() == 1) {
            // Only one reviewer graded so far
            EvaluationReview single = reviews.get(0);
            double score = getReviewScore(single);
            answer.setScore(score);
            answer.setEvaluatorFeedback(single.getFeedback());
            answer.setEvaluatedAt(OffsetDateTime.now());
            answer.setRubricScores(single.getScores());
            answerRepository.save(answer);
            checkAndFinalizeSession(session);
            return;
        }

        // Multiple reviewers
        double minScore = Double.MAX_VALUE;
        double maxScore = Double.MIN_VALUE;
        double sumScore = 0.0;
        StringBuilder combinedFeedback = new StringBuilder("Combined Reviewer Feedback:\n");

        for (EvaluationReview rev : reviews) {
            double score = getReviewScore(rev);
            if (score < minScore) minScore = score;
            if (score > maxScore) maxScore = score;
            sumScore += score;
            combinedFeedback.append("- Evaluator #")
                    .append(rev.getEvaluatorId())
                    .append(": ")
                    .append(rev.getFeedback())
                    .append("\n");
        }

        double deviationPercentage = (maxScore - minScore) / maxQuestionScore;

        if (deviationPercentage <= 0.15) {
            // Margin is 15% or less, average the scores
            double avgScore = sumScore / reviews.size();
            answer.setScore(avgScore);
            answer.setEvaluatorFeedback(combinedFeedback.toString());
            answer.setEvaluatedAt(OffsetDateTime.now());
            // Combine all rubric scores
            Map<String, Object> combinedRubrics = new HashMap<>();
            for (EvaluationReview rev : reviews) {
                if (rev.getScores() != null) {
                    combinedRubrics.putAll(rev.getScores());
                }
            }
            combinedRubrics.put("score", avgScore);
            answer.setRubricScores(combinedRubrics);
            answerRepository.save(answer);

            checkAndFinalizeSession(session);
        } else {
            // Flags for manual resolution because discrepancy exceeds 15%
            answer.setScore(null); // Keep score unassigned
            answer.setEvaluatorFeedback("[FLAGGED FOR MANUAL RESOLUTION] Score discrepancy exceeds 15%:\n" + combinedFeedback.toString());
            answerRepository.save(answer);
            log.warn("Answer #{} flagged for manual resolution due to score deviation: {}%", answerId, deviationPercentage * 100);
        }
    }

    private double getReviewScore(EvaluationReview review) {
        if (review.getScores() == null) return 0.0;
        Object scoreObj = review.getScores().get("score");
        if (scoreObj instanceof Number) {
            return ((Number) scoreObj).doubleValue();
        }
        return 0.0;
    }

    private void checkAndFinalizeSession(AssessmentSession session) {
        List<SessionAnswer> answers = answerRepository.findBySessionId(session.getId());
        
        // If there are any pending subjective answers that have no score and are not auto graded
        boolean allGraded = answers.stream()
                .filter(a -> !a.isAutoGraded())
                .allMatch(a -> a.getScore() != null);

        if (allGraded) {
            double totalScore = answers.stream()
                    .mapToDouble(a -> a.getScore() != null ? a.getScore() : 0.0)
                    .sum();

            session.setTotalScore(totalScore);
            session.setStatus("GRADED");
            sessionRepository.save(session);
            log.info("Assessment session #{} finalized. Total score: {}", session.getId(), totalScore);

            // Notify candidate
            notificationService.notifyCandidateOfResult(session.getUserId(), "Assessment Results", totalScore);

            // Auto-issue Certification if assessment is certifying and score >= passingScore
            try {
                com.CodeSphere.backend.entity.Assessment assessment = assessmentRepository.findById(session.getAssessmentId()).orElse(null);
                if (assessment != null && assessment.isCertifying()) {
                    double maxPossible = session.getMaxPossibleScore() != null ? session.getMaxPossibleScore() : 100.0;
                    double percentage = (totalScore / maxPossible) * 100.0;
                    double passingScore = assessment.getPassingScore() != null ? assessment.getPassingScore() : 50.0;

                    if (percentage >= passingScore) {
                        certificationService.issueCertificate(
                                session.getUserId(),
                                assessment.getTitle(),
                                session.getId(),
                                percentage
                        );
                        log.info("Auto-issued certificate for user ID {} on passing assessment '{}'",
                                session.getUserId(), assessment.getTitle());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to auto-issue certification: {}", e.getMessage());
            }
        }
    }
}
