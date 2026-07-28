package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.evaluation.EvaluationQueueItem;
import com.CodeSphere.backend.dto.evaluation.EvaluationRequest;
import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
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

@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
public class EvaluationController {
    private final AssessmentAnswerRepository answerRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final QuestionBankRepository questionRepository;

    @GetMapping("/pending")
    @Transactional(readOnly = true)
    public ResponseEntity<List<EvaluationQueueItem>> getPending() {
        return ResponseEntity.ok(answerRepository.findByEvaluationStatusOrderByUpdatedAtAsc("PENDING_EVALUATION").stream()
                .map(this::toQueueItem).toList());
    }

    @PutMapping("/{answerId}")
    public ResponseEntity<Void> evaluate(@PathVariable Long answerId, @Valid @RequestBody EvaluationRequest request,
                                         Authentication authentication) {
        AssessmentAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new IllegalArgumentException("Answer not found"));
        if (!"PENDING_EVALUATION".equals(answer.getEvaluationStatus())) {
            throw new IllegalStateException("This answer is not awaiting evaluation");
        }
        Long evaluatorId = authentication.getPrincipal() instanceof CustomUserDetails user ? user.getId() : null;
        answer.setScore(request.getScore());
        answer.setEvaluatorFeedback(request.getFeedback());
        answer.setEvaluatedBy(evaluatorId);
        answer.setEvaluatedAt(OffsetDateTime.now());
        answer.setEvaluationStatus("EVALUATED");
        answerRepository.save(answer);
        return ResponseEntity.noContent().build();
    }

    private EvaluationQueueItem toQueueItem(AssessmentAnswer answer) {
        AssessmentSession session = sessionRepository.findById(answer.getSessionId())
                .orElseThrow(() -> new IllegalStateException("Session not found"));
        Question question = questionRepository.findById(answer.getQuestionId())
                .orElseThrow(() -> new IllegalStateException("Question not found"));
        return EvaluationQueueItem.builder().answerId(answer.getId()).sessionId(answer.getSessionId())
                .questionId(question.getId()).questionTitle(question.getTitle()).questionType(question.getQuestionType())
                .candidateName(session.getCandidate().getUsername()).answerText(answer.getSelectedAnswer()).fileUrl(answer.getFileUrl())
                .maxScore(question.getPoints()).build();
    }
}
