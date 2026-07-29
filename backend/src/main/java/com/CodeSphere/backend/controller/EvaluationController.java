package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.evaluation.EvaluationQueueItem;
import com.CodeSphere.backend.dto.evaluation.EvaluationRequest;
import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.RubricRepository;
import com.CodeSphere.backend.repository.AssessmentEvaluationReviewRepository;
import com.CodeSphere.backend.model.AssessmentEvaluationReview;
import com.CodeSphere.backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
public class EvaluationController {
    private final AssessmentAnswerRepository answerRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final QuestionBankRepository questionRepository;
    private final RubricRepository rubricRepository;
    private final AssessmentEvaluationReviewRepository reviewRepository;
    private static final int REQUIRED_REVIEW_COUNT = 2;

    @GetMapping("/pending")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EvaluationQueueItem>> getPending(Authentication authentication) {
        Long evaluatorId = authentication.getPrincipal() instanceof CustomUserDetails user ? user.getId() : null;
        List<AssessmentAnswer> answers = new java.util.ArrayList<>();
        answers.addAll(answerRepository.findByEvaluationStatusOrderByUpdatedAtAsc("PENDING_EVALUATION"));
        answers.addAll(answerRepository.findByEvaluationStatusOrderByUpdatedAtAsc("UNDER_REVIEW"));
        return ResponseEntity.ok(answers.stream().filter(answer -> evaluatorId != null && reviewRepository.findByAnswerIdAndEvaluatorId(answer.getId(), evaluatorId).isEmpty())
                .map(this::toQueueItem).toList());
    }

    @PutMapping("/{answerId}")
    public ResponseEntity<Void> evaluate(@PathVariable Long answerId, @Valid @RequestBody EvaluationRequest request,
                                         Authentication authentication) {
        AssessmentAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found"));
        if (!"PENDING_EVALUATION".equals(answer.getEvaluationStatus()) && !"UNDER_REVIEW".equals(answer.getEvaluationStatus())) {
            throw new IllegalStateException("This answer is not awaiting evaluation");
        }
        Long evaluatorId = authentication.getPrincipal() instanceof CustomUserDetails user ? user.getId() : null;
        if (evaluatorId == null) throw new IllegalStateException("Evaluator identity is unavailable");
        if (reviewRepository.findByAnswerIdAndEvaluatorId(answerId, evaluatorId).isPresent()) throw new IllegalStateException("You already reviewed this answer");
        reviewRepository.save(AssessmentEvaluationReview.builder().answerId(answerId).evaluatorId(evaluatorId)
                .score(request.getScore()).feedback(request.getFeedback()).rubricScores(request.getRubricScores()).reviewedAt(OffsetDateTime.now()).build());
        List<AssessmentEvaluationReview> reviews = reviewRepository.findByAnswerIdOrderByReviewedAtAsc(answerId);
        BigDecimal consensus = reviews.stream().map(AssessmentEvaluationReview::getScore).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(reviews.size()), 2, java.math.RoundingMode.HALF_UP);
        answer.setScore(consensus);
        answer.setEvaluatorFeedback(reviews.stream().map(AssessmentEvaluationReview::getFeedback).filter(value -> value != null && !value.isBlank()).collect(java.util.stream.Collectors.joining("\n\n")));
        answer.setEvaluatedBy(evaluatorId);
        answer.setEvaluatedAt(OffsetDateTime.now());
        answer.setEvaluationStatus(reviews.size() >= REQUIRED_REVIEW_COUNT ? "EVALUATED" : "UNDER_REVIEW");
        answerRepository.save(answer);
        return ResponseEntity.noContent().build();
    }

    private EvaluationQueueItem toQueueItem(AssessmentAnswer answer) {
        AssessmentSession session = sessionRepository.findById(answer.getSessionId())
                .orElseThrow(() -> new IllegalStateException("Session not found"));
        Question question = questionRepository.findById(answer.getQuestionId())
                .orElseThrow(() -> new IllegalStateException("Question not found"));
        List<EvaluationQueueItem.RubricCriterionItem> rubricCriteria = rubricRepository.findById(question.getId())
                .map(rubric -> rubric.getCriteria().stream().map(criterion -> EvaluationQueueItem.RubricCriterionItem.builder()
                        .name(criterion.getCriterionName()).maxPoints(criterion.getMaxPoints()).build()).toList())
                .orElse(List.of());
        int reviewCount = reviewRepository.findByAnswerIdOrderByReviewedAtAsc(answer.getId()).size();
        return EvaluationQueueItem.builder().answerId(answer.getId()).sessionId(answer.getSessionId())
                .questionId(question.getId()).questionTitle(question.getTitle()).questionType(question.getQuestionType())
                .candidateName(session.getCandidate().getUsername()).answerText(answer.getSelectedAnswer()).fileUrl(answer.getFileUrl())
                .maxScore(question.getPoints()).rubricCriteria(rubricCriteria).reviewCount(reviewCount).requiredReviewCount(REQUIRED_REVIEW_COUNT).build();
    }
}
