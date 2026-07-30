package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.SqlRunRequest;
import com.CodeSphere.backend.dto.SqlRunResponse;
import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.security.CustomUserDetails;
import com.CodeSphere.backend.service.SqlAssessmentRunnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/sql-assessments")
@RequiredArgsConstructor
public class SqlAssessmentController {
    private final AssessmentSessionRepository sessionRepository;
    private final QuestionBankRepository questionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final SqlAssessmentRunnerService sqlRunner;

    @PostMapping("/run")
    @Transactional
    public ResponseEntity<SqlRunResponse> run(@Valid @RequestBody SqlRunRequest request) {
        Long userId = currentUserId();
        AssessmentSession session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new IllegalArgumentException("Assessment session not found"));
        if (!userId.equals(session.getCandidate().getId()) || session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS)
            return ResponseEntity.status(403).build();
        if (!session.getQuestionIdsSnapshot().contains(request.questionId()))
            throw new IllegalArgumentException("Question does not belong to this assessment session");
        var question = questionRepository.findById(request.questionId()).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        if (!"SQL".equals(question.getQuestionType())) throw new IllegalArgumentException("This is not an SQL assessment task");

        SqlRunResponse result = sqlRunner.run(question, request.sql());
        double maxScore = assessmentQuestionRepository.findByAssessmentId(session.getAssessmentId()).stream()
                .filter(mapping -> request.questionId().equals(mapping.getQuestionBankId()))
                .map(mapping -> mapping.getMaxScore() == null ? 0D : mapping.getMaxScore().doubleValue()).findFirst().orElse(0D);
        AssessmentAnswer answer = answerRepository.findBySessionIdAndQuestionId(session.getId(), request.questionId()).orElseGet(AssessmentAnswer::new);
        answer.setSessionId(session.getId()); answer.setQuestionId(request.questionId()); answer.setSelectedAnswer(request.sql());
        answer.setScore(BigDecimal.valueOf(maxScore * result.scorePercent() / 100D));
        answer.setEvaluationStatus("EVALUATED"); answer.setUpdatedAt(LocalDateTime.now());
        answerRepository.save(answer);
        return ResponseEntity.ok(result);
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUserDetails user) return user.getId();
        throw new IllegalStateException("Authenticated user details are unavailable.");
    }
}
