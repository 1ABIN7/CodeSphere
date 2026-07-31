package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.ApiRunRequest;
import com.CodeSphere.backend.dto.ApiRunResponse;
import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.repository.*;
import com.CodeSphere.backend.security.CustomUserDetails;
import com.CodeSphere.backend.service.ApiAssessmentRunnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/api-assessments")
@RequiredArgsConstructor
public class ApiAssessmentController {
    private final AssessmentSessionRepository sessionRepository;
    private final QuestionBankRepository questionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final ApiAssessmentRunnerService runner;

    @PostMapping("/run") @Transactional
    public ResponseEntity<ApiRunResponse> run(@Valid @RequestBody ApiRunRequest request) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails user)) throw new IllegalStateException("Authenticated user details are unavailable.");
        AssessmentSession session = sessionRepository.findById(request.sessionId()).orElseThrow(() -> new IllegalArgumentException("Assessment session not found"));
        if (!user.getId().equals(session.getCandidate().getId()) || session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) return ResponseEntity.status(403).build();
        if (!session.getQuestionIdsSnapshot().contains(request.questionId())) throw new IllegalArgumentException("Question does not belong to this assessment session");
        var question = questionRepository.findById(request.questionId()).orElseThrow(() -> new IllegalArgumentException("Question not found"));
        if (!"API_IMPLEMENTATION".equals(question.getQuestionType())) throw new IllegalArgumentException("This is not an API implementation task");
        ApiRunResponse result = runner.run(question, request.code());
        if (!"SANDBOX_UNAVAILABLE".equals(result.status())) {
            double max = assessmentQuestionRepository.findByAssessmentId(session.getAssessmentId()).stream().filter(m -> request.questionId().equals(m.getQuestionBankId())).map(m -> m.getMaxScore() == null ? 0D : m.getMaxScore().doubleValue()).findFirst().orElse(0D);
            AssessmentAnswer answer = answerRepository.findBySessionIdAndQuestionId(session.getId(), request.questionId()).orElseGet(AssessmentAnswer::new);
            answer.setSessionId(session.getId()); answer.setQuestionId(request.questionId()); answer.setSelectedAnswer(request.code()); answer.setScore(BigDecimal.valueOf(max * result.scorePercent() / 100D)); answer.setEvaluationStatus("EVALUATED"); answer.setUpdatedAt(LocalDateTime.now()); answerRepository.save(answer);
        }
        return ResponseEntity.ok(result);
    }
}
