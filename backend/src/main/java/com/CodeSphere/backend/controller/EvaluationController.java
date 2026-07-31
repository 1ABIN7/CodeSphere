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
import com.CodeSphere.backend.model.AssessmentEvaluatorAssignment;
import com.CodeSphere.backend.model.Role;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.dto.evaluation.EvaluatorAssignmentRequest;
import com.CodeSphere.backend.dto.evaluation.ResolveEvaluationRequest;
import com.CodeSphere.backend.repository.AssessmentEvaluatorAssignmentRepository;
import com.CodeSphere.backend.repository.UserRepository;
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
    private final AssessmentEvaluatorAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private static final int REQUIRED_REVIEW_COUNT = 2;

    @GetMapping("/pending")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EvaluationQueueItem>> getPending(Authentication authentication) {
        Long evaluatorId = authentication.getPrincipal() instanceof CustomUserDetails user ? user.getId() : null;
        List<AssessmentAnswer> answers = new java.util.ArrayList<>();
        answers.addAll(answerRepository.findByEvaluationStatusOrderByUpdatedAtAsc("PENDING_EVALUATION"));
        answers.addAll(answerRepository.findByEvaluationStatusOrderByUpdatedAtAsc("UNDER_REVIEW"));
        return ResponseEntity.ok(answers.stream().filter(answer -> evaluatorId != null && reviewRepository.findByAnswerIdAndEvaluatorId(answer.getId(), evaluatorId).isEmpty()
                        && (assignmentRepository.findByAnswerId(answer.getId()).isEmpty() || assignmentRepository.existsByAnswerIdAndEvaluatorId(answer.getId(), evaluatorId)))
                .map(this::toQueueItem).toList());
    }

    @GetMapping("/management")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EvaluationQueueItem>> managementQueue() {
        List<AssessmentAnswer> answers = new java.util.ArrayList<>();
        answers.addAll(answerRepository.findByEvaluationStatusOrderByUpdatedAtAsc("PENDING_EVALUATION"));
        answers.addAll(answerRepository.findByEvaluationStatusOrderByUpdatedAtAsc("UNDER_REVIEW"));
        return ResponseEntity.ok(answers.stream().map(this::toQueueItem).toList());
    }

    @GetMapping("/evaluators")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<List<java.util.Map<String, Object>>> evaluators() {
        return ResponseEntity.ok(userRepository.findByRoleIn(List.of(Role.ROLE_EXAMINER, Role.ROLE_ORG_ADMIN, Role.ROLE_SUPER_ADMIN)).stream()
                .map(user -> java.util.Map.<String, Object>of("id", user.getId(), "name", user.getFullName() == null || user.getFullName().isBlank() ? user.getUsername() : user.getFullName(), "role", user.getRole().name())).toList());
    }

    @PutMapping("/{answerId}/assignments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    @Transactional
    public ResponseEntity<Void> assign(@PathVariable Long answerId, @Valid @RequestBody EvaluatorAssignmentRequest request, Authentication authentication) {
        AssessmentAnswer answer = answerRepository.findById(answerId).orElseThrow(() -> new IllegalArgumentException("Answer not found"));
        if (!"PENDING_EVALUATION".equals(answer.getEvaluationStatus()) && !"UNDER_REVIEW".equals(answer.getEvaluationStatus())) throw new IllegalStateException("This answer is no longer awaiting evaluation");
        Long adminId = ((CustomUserDetails) authentication.getPrincipal()).getId();
        List<Long> evaluatorIds = request.evaluatorIds().stream().distinct().toList();
        if (evaluatorIds.size() > REQUIRED_REVIEW_COUNT) throw new IllegalArgumentException("Assign at most two evaluators");
        List<User> users = userRepository.findAllById(evaluatorIds);
        if (users.size() != evaluatorIds.size() || users.stream().anyMatch(user -> !List.of(Role.ROLE_EXAMINER, Role.ROLE_ORG_ADMIN, Role.ROLE_SUPER_ADMIN).contains(user.getRole()))) throw new IllegalArgumentException("Assignments must be valid evaluator accounts");
        assignmentRepository.deleteByAnswerId(answerId);
        evaluatorIds.forEach(id -> assignmentRepository.save(AssessmentEvaluatorAssignment.builder().answerId(answerId).evaluatorId(id).assignedBy(adminId).assignedAt(OffsetDateTime.now()).build()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{answerId}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<Void> resolve(@PathVariable Long answerId, @Valid @RequestBody ResolveEvaluationRequest request, Authentication authentication) {
        AssessmentAnswer answer = answerRepository.findById(answerId).orElseThrow(() -> new IllegalArgumentException("Answer not found"));
        List<AssessmentEvaluationReview> reviews = reviewRepository.findByAnswerIdOrderByReviewedAtAsc(answerId);
        if (reviews.size() < 2) throw new IllegalStateException("Two reviewer submissions are required before resolving a disagreement");
        answer.setScore(request.score()); answer.setEvaluatorFeedback((answer.getEvaluatorFeedback() == null ? "" : answer.getEvaluatorFeedback() + "\n\n") + "Admin resolution: " + (request.feedback() == null ? "" : request.feedback()));
        answer.setEvaluatedBy(((CustomUserDetails) authentication.getPrincipal()).getId()); answer.setEvaluatedAt(OffsetDateTime.now()); answer.setEvaluationStatus("EVALUATED"); answerRepository.save(answer);
        return ResponseEntity.noContent().build();
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
        if (!assignmentRepository.findByAnswerId(answerId).isEmpty() && !assignmentRepository.existsByAnswerIdAndEvaluatorId(answerId, evaluatorId)) throw new IllegalStateException("This answer is assigned to a different evaluator");
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
        List<EvaluationQueueItem.RubricCriterionItem> rubricCriteria = rubricRepository.findByQuestionId(question.getId())
                .map(rubric -> rubric.getCriteria().stream().map(criterion -> EvaluationQueueItem.RubricCriterionItem.builder()
                        .name(criterion.getCriterionName()).maxPoints(criterion.getMaxPoints()).build()).toList())
                .orElse(List.of());
        List<AssessmentEvaluationReview> reviews = reviewRepository.findByAnswerIdOrderByReviewedAtAsc(answer.getId());
        int reviewCount = reviews.size();
        return EvaluationQueueItem.builder().answerId(answer.getId()).sessionId(answer.getSessionId())
                .questionId(question.getId()).questionTitle(question.getTitle()).questionType(question.getQuestionType()).questionContent(question.getContent())
                .candidateName(session.getCandidate().getUsername()).answerText(answer.getSelectedAnswer()).fileUrl(answer.getFileUrl())
                .maxScore(question.getPoints()).rubricCriteria(rubricCriteria).reviewCount(reviewCount).requiredReviewCount(REQUIRED_REVIEW_COUNT)
                .assignedEvaluatorIds(assignmentRepository.findByAnswerId(answer.getId()).stream().map(AssessmentEvaluatorAssignment::getEvaluatorId).toList())
                .reviews(reviews.stream().map(review -> EvaluationQueueItem.ReviewItem.builder().evaluatorId(review.getEvaluatorId()).score(review.getScore()).feedback(review.getFeedback()).build()).toList()).build();
    }
}
