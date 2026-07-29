package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.AssessmentResultDTO;
import com.CodeSphere.backend.dto.AssessmentHistoryItemDto;
import com.CodeSphere.backend.dto.SectionResultDto;
import com.CodeSphere.backend.dto.SubmissionDTO;
import com.CodeSphere.backend.service.AssessmentService;
import com.CodeSphere.backend.service.AuditLogService;
import com.CodeSphere.backend.service.AssessmentSessionService;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.security.CustomUserDetails;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSectionRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.service.McqEvaluationService;
import com.CodeSphere.backend.service.FileUploadAssessmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/assessment-sessions")
@RequiredArgsConstructor
public class AssessmentSessionController {

    private final AssessmentService assessmentService;
    private final AuditLogService auditLogService;
    private final AssessmentSessionService assessmentSessionService;
    private final AssessmentSessionRepository sessionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final AssessmentRepository assessmentRepository;
    private final McqEvaluationService mcqEvaluationService;
    private final FileUploadAssessmentService fileUploadAssessmentService;
    private final AssessmentSectionRepository assessmentSectionRepository;
    private final QuestionBankRepository questionBankRepository;

    /**
     * Initiates a live assessment session for the authenticated user and logs the critical action.
     */
    @PostMapping("/{assessmentId}/start")
    public ResponseEntity<AssessmentSession> startAssessmentSession(@PathVariable Long assessmentId, HttpServletRequest request) {
        Long userId = getCurrentUserId();

        AssessmentSession session = assessmentService.start(assessmentId, userId);

        // [Audit Log] Log live user attempt initialization
        auditLogService.logAction(
                userId,
                "ASSESSMENT-START",
                "Assessment ID: " + assessmentId,
                request
        );

        return ResponseEntity.ok(session);
    }

    /**
     * Submits an active assessment session, processes scores, and logs the critical submission action.
     */
    @PostMapping("/{assessmentId}/submit")
    public ResponseEntity<AssessmentResultDTO> submitAssessmentSession(@PathVariable Long assessmentId,
                                                                       @RequestBody SubmissionDTO submission,
                                                                       HttpServletRequest request) {
        Long userId = getCurrentUserId();

        AssessmentResultDTO result = (AssessmentResultDTO) assessmentService.submit(assessmentId, userId, submission);

        // [Audit Log] Log critical test completion state change
        auditLogService.logAction(
                userId,
                "ASSESSMENT-SUBMIT",
                "Assessment ID: " + assessmentId,
                request
        );

        return ResponseEntity.ok(result);
    }

    @PostMapping("/{sessionId}/answers")
    public ResponseEntity<?> autosaveAnswer(@PathVariable Long sessionId, @RequestBody AnswerSaveRequest request) {
        return ResponseEntity.ok(assessmentSessionService.autoSaveAnswer(sessionId, request.questionId(), request.value()));
    }

    /** Server-authoritative section transition.  The returned session contains
     * the server start timestamp used by the browser to render its countdown. */
    @PostMapping("/{sessionId}/sections/{sectionIndex}/navigate")
    public ResponseEntity<AssessmentSession> navigateSection(@PathVariable Long sessionId, @PathVariable int sectionIndex) {
        Long userId = getCurrentUserId();
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment session not found"));
        if (!userId.equals(session.getCandidate().getId())) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(assessmentSessionService.navigateToSection(sessionId, sectionIndex));
    }

    @PostMapping("/{sessionId}/files")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> uploadAssessmentFile(@PathVariable Long sessionId, @RequestParam Long questionId,
                                                   @RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        var session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment session not found"));
        if (!userId.equals(session.getCandidate().getId()) || session.getStatus() != AssessmentSession.SessionStatus.IN_PROGRESS) {
            return ResponseEntity.status(403).body(java.util.Map.of("message", "This assessment session is not available for uploads."));
        }
        if (!session.getQuestionIdsSnapshot().contains(questionId)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Question does not belong to this assessment."));
        }
        return ResponseEntity.ok(fileUploadAssessmentService.uploadAndQueueFile(sessionId, questionId, file));
    }

    public record AnswerSaveRequest(Long questionId, String value) {}

    @GetMapping("/results")
    public ResponseEntity<java.util.List<AssessmentHistoryItemDto>> getResultHistory() {
        Long userId = getCurrentUserId();
        return ResponseEntity.ok(sessionRepository.findByCandidateIdAndStatusOrderBySubmittedAtDesc(userId, AssessmentSession.SessionStatus.SUBMITTED)
                .stream().filter(session -> assessmentRepository.findById(session.getAssessmentId()).map(assessment -> assessment.isResultsVisible()).orElse(false))
                .map(this::toHistoryItem).toList());
    }

    @GetMapping("/{assessmentId}/result")
    public ResponseEntity<AssessmentResultDTO> getResult(@PathVariable Long assessmentId) {
        Long userId = getCurrentUserId();
        AssessmentSession session = sessionRepository.findByAssessmentIdAndCandidateIdAndStatus(assessmentId, userId, AssessmentSession.SessionStatus.SUBMITTED)
                .orElseThrow(() -> new IllegalArgumentException("No submitted result found for this assessment"));
        double autoScore = mcqEvaluationService.evaluateSessionMcqs(session);
        double manualScore = answerRepository.findBySessionId(session.getId()).stream()
                .filter(answer -> "EVALUATED".equals(answer.getEvaluationStatus()) && answer.getScore() != null)
                .mapToDouble(answer -> answer.getScore().doubleValue()).sum();
        boolean pending = answerRepository.findBySessionId(session.getId()).stream()
                .anyMatch(answer -> "PENDING_EVALUATION".equals(answer.getEvaluationStatus()) || "UNDER_REVIEW".equals(answer.getEvaluationStatus()));
        double total = assessmentQuestionRepository.findByAssessmentId(assessmentId).stream()
                .filter(mapping -> mapping.getMaxScore() != null).mapToDouble(mapping -> mapping.getMaxScore()).sum();
        var assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found"));
        if (!assessment.isResultsVisible()) throw new IllegalStateException("Results are not available for this assessment yet.");
        java.util.List<String> feedback = answerRepository.findBySessionId(session.getId()).stream()
                .map(answer -> answer.getEvaluatorFeedback()).filter(java.util.Objects::nonNull)
                .filter(value -> !value.isBlank()).toList();
        double score = autoScore + manualScore;
        return ResponseEntity.ok(AssessmentResultDTO.builder().sessionId(session.getId()).userId(userId)
                .assessmentId(assessmentId).assessmentTitle(assessment.getTitle()).score(score).totalScore(total)
                .passingScore(assessment.getPassingScore()).passed(!pending && (assessment.getPassingScore() == null || score >= assessment.getPassingScore()))
                .evaluatorFeedback(assessment.isFeedbackVisible() ? feedback : java.util.List.of()).sections(sectionResults(session)).status(pending ? "PENDING_EVALUATION" : "COMPLETED").build());
    }

    private java.util.List<SectionResultDto> sectionResults(AssessmentSession session) {
        java.util.Map<Long, com.CodeSphere.backend.model.AssessmentAnswer> answers = answerRepository.findBySessionId(session.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(com.CodeSphere.backend.model.AssessmentAnswer::getQuestionId, answer -> answer, (left, right) -> right));
        return assessmentSectionRepository.findByAssessmentIdOrderBySectionOrderAsc(session.getAssessmentId()).stream().map(section -> {
            java.util.List<com.CodeSphere.backend.entity.AssessmentQuestion> mappings = assessmentQuestionRepository.findBySectionIdOrderByOrderIndexAsc(section.getId());
            double total = mappings.stream().map(com.CodeSphere.backend.entity.AssessmentQuestion::getMaxScore)
                    .filter(java.util.Objects::nonNull).mapToDouble(Double::doubleValue).sum();
            double score = mappings.stream().mapToDouble(mapping -> {
                var answer = answers.get(mapping.getQuestionBankId());
                var question = questionBankRepository.findById(mapping.getQuestionBankId()).orElse(null);
                if (answer == null || question == null) return 0D;
                if ("EVALUATED".equals(answer.getEvaluationStatus()) && answer.getScore() != null) return answer.getScore().doubleValue();
                String submitted = answer.getSelectedAnswer() == null ? "" : answer.getSelectedAnswer().replaceAll("\\s", "").toUpperCase();
                String expected = question.getCorrectAnswers() == null ? "" : question.getCorrectAnswers().replaceAll("\\s", "").toUpperCase();
                if (("MCQ_SINGLE".equals(question.getQuestionType()) || "MCQ_MULTI".equals(question.getQuestionType())) && !expected.isBlank())
                    return submitted.equals(expected) ? mapping.getMaxScore() : -Math.min(mapping.getNegativeScore() == null ? 0D : mapping.getNegativeScore(), mapping.getMaxScore());
                return 0D;
            }).sum();
            return SectionResultDto.builder().sectionId(section.getId()).title(section.getTitle()).score(score).totalScore(total).build();
        }).toList();
    }

    private AssessmentHistoryItemDto toHistoryItem(AssessmentSession session) {
        double autoScore = mcqEvaluationService.evaluateSessionMcqs(session);
        java.util.List<com.CodeSphere.backend.model.AssessmentAnswer> answers = answerRepository.findBySessionId(session.getId());
        double manualScore = answers.stream().filter(answer -> "EVALUATED".equals(answer.getEvaluationStatus()) && answer.getScore() != null)
                .mapToDouble(answer -> answer.getScore().doubleValue()).sum();
        boolean pending = answers.stream().anyMatch(answer -> "PENDING_EVALUATION".equals(answer.getEvaluationStatus()) || "UNDER_REVIEW".equals(answer.getEvaluationStatus()));
        double total = assessmentQuestionRepository.findByAssessmentId(session.getAssessmentId()).stream()
                .filter(mapping -> mapping.getMaxScore() != null).mapToDouble(mapping -> mapping.getMaxScore()).sum();
        String title = assessmentRepository.findById(session.getAssessmentId()).map(assessment -> assessment.getTitle()).orElse("Assessment");
        return new AssessmentHistoryItemDto(session.getAssessmentId(), title, autoScore + manualScore, total,
                pending ? "PENDING_EVALUATION" : "COMPLETED", session.getSubmittedAt());
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Authentication context is missing or invalid.");
        }
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getId();
        }
        throw new IllegalStateException("Authenticated user details are unavailable.");
    }
}
