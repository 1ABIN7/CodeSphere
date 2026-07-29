package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.dto.AssessmentResultDTO;
import com.CodeSphere.backend.dto.SubmissionDTO;
import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import com.CodeSphere.backend.entity.AssessmentQuestion;
import com.CodeSphere.backend.model.AssessmentSection;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.model.User;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.AssessmentAssignmentRepository;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.repository.AssessmentRepository;
import com.CodeSphere.backend.repository.AssessmentSectionRepository;
import com.CodeSphere.backend.repository.AssessmentSessionRepository;
import com.CodeSphere.backend.repository.UserRepository;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.service.McqEvaluationService;
import com.CodeSphere.backend.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentSectionRepository sectionRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentAssignmentRepository assignmentRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final UserRepository userRepository;
    private final AssessmentAnswerRepository answerRepository;
    private final QuestionBankRepository questionBankRepository;
    private final McqEvaluationService mcqEvaluationService;

    // ==========================================
    // Admin & Lifecycle Methods
    // ==========================================

    @Override
    public Assessment createAssessment(Assessment assessment) {
        assessment.setPublished(false);
        return assessmentRepository.save(assessment);
    }

    @Override
    public Assessment updateAssessment(Long id, Assessment assessmentDetails) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id " + id));

        assessment.setTitle(assessmentDetails.getTitle());
        assessment.setDescription(assessmentDetails.getDescription());
        assessment.setAssessmentType(assessmentDetails.getAssessmentType());
        assessment.setDurationMinutes(assessmentDetails.getDurationMinutes());
        assessment.setStartTime(assessmentDetails.getStartTime());
        assessment.setEndTime(assessmentDetails.getEndTime());
        assessment.setPassingScore(assessmentDetails.getPassingScore());
        assessment.setInstructions(assessmentDetails.getInstructions());
        assessment.setShuffleQuestions(assessmentDetails.isShuffleQuestions());
        assessment.setShuffleOptions(assessmentDetails.isShuffleOptions());
        assessment.setAllowResume(assessmentDetails.isAllowResume());
        assessment.setCertifying(assessmentDetails.isCertifying());
        assessment.setAccessCode(assessmentDetails.getAccessCode());

        return assessmentRepository.save(assessment);
    }

    @Override
    @Transactional
    public void deleteAssessment(Long id) {
        Assessment assessment = assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id " + id));
        questionRepository.deleteByAssessmentId(id);
        sectionRepository.deleteByAssessmentId(id);
        assessmentRepository.delete(assessment);
    }

    @Override
    public Assessment getAssessmentById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id " + id));
    }

    @Override
    public List<Assessment> getAllAssessments() {
        return assessmentRepository.findAll();
    }

    @Override
    @Transactional
    public Assessment cloneAssessment(Long id) {
        Assessment original = getAssessmentById(id);

        Assessment cloned = Assessment.builder()
                .title(original.getTitle() + " - Clone")
                .description(original.getDescription())
                .assessmentType(original.getAssessmentType())
                .organizationId(original.getOrganizationId())
                .createdBy(original.getCreatedBy())
                .durationMinutes(original.getDurationMinutes())
                .startTime(original.getStartTime())
                .endTime(original.getEndTime())
                .passingScore(original.getPassingScore())
                .instructions(original.getInstructions())
                .shuffleQuestions(original.isShuffleQuestions())
                .shuffleOptions(original.isShuffleOptions())
                .allowResume(original.isAllowResume())
                .isCertifying(original.isCertifying())
                .isPublished(false)
                .accessCode(original.getAccessCode())
                .build();

        Assessment savedClone = assessmentRepository.save(cloned);

        List<AssessmentSection> originalSections = sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(original.getId());
        for (AssessmentSection origSec : originalSections) {
            AssessmentSection clonedSec = AssessmentSection.builder()
                    .assessmentId(savedClone.getId())
                    .title(origSec.getTitle())
                    .sectionOrder(origSec.getSectionOrder())
                    .durationMinutes(origSec.getDurationMinutes()) // ✅ Uses durationMinutes field on builder
                    .sectionType(origSec.getSectionType())
                    .navigationMode(origSec.getNavigationMode())
                    .build();
            AssessmentSection savedClonedSec = sectionRepository.save(clonedSec);

            List<AssessmentQuestion> originalQuestions = questionRepository.findBySectionIdOrderByOrderIndexAsc(origSec.getId());
            for (AssessmentQuestion origQues : originalQuestions) {
                AssessmentQuestion clonedQues = AssessmentQuestion.builder()
                        .assessmentId(savedClone.getId())
                        .sectionId(savedClonedSec.getId())
                        .questionBankId(origQues.getQuestionBankId())
                        .orderIndex(origQues.getOrderIndex())
                        .maxScore(origQues.getMaxScore())
                        .negativeScore(origQues.getNegativeScore())
                        .timeLimitOverride(origQues.getTimeLimitOverride())
                        .build();
                questionRepository.save(clonedQues);
            }
        }

        return savedClone;
    }

    @Override
    public Assessment publishAssessment(Long id) {
        Assessment assessment = getAssessmentById(id);

        List<AssessmentSection> sections = sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(id);
        if (sections.isEmpty()) {
            throw new IllegalStateException("Cannot publish an assessment with no sections");
        }

        for (AssessmentSection section : sections) {
            List<AssessmentQuestion> questions = questionRepository.findBySectionIdOrderByOrderIndexAsc(section.getId());
            if (questions.isEmpty()) {
                throw new IllegalStateException("Section '" + section.getTitle() + "' has no questions mapped");
            }
        }

        assessment.setPublished(true);
        return assessmentRepository.save(assessment);
    }

    @Override
    public Assessment unpublishAssessment(Long id) {
        Assessment assessment = getAssessmentById(id);
        assessment.setPublished(false);
        return assessmentRepository.save(assessment);
    }

    @Override
    public AssessmentAssignment assignAssessment(Long id, Long userId, LocalDateTime deadline) {
        getAssessmentById(id);

        AssessmentAssignment assignment = AssessmentAssignment.builder()
                .assessmentId(id)
                .userId(userId)
                .deadline(deadline)
                .build();

        return assignmentRepository.save(assignment);
    }

    @Override
    public List<AssessmentAssignment> getAssignedCandidates(Long id) {
        return assignmentRepository.findByAssessmentId(id);
    }

    // ==========================================
    // Session Execution & Scoring Methods
    // ==========================================

    @Override
    @Transactional(readOnly = true)
    public Object findById(Long assessmentId) {
        return getAssessmentById(assessmentId);
    }

    @Override
    @Transactional
    public Object createBlueprint(Object assessmentDto) {
        return assessmentDto;
    }

    @Override
    @Transactional
    public AssessmentSession start(Long assessmentId, Long userId) {
        Assessment assessment = getAssessmentById(assessmentId);
        if (!assessment.isPublished()) {
            throw new IllegalStateException("This assessment is not published");
        }
        LocalDateTime now = LocalDateTime.now();
        if (assessment.getStartTime() != null && now.isBefore(assessment.getStartTime())) {
            throw new IllegalStateException("This assessment has not opened yet");
        }
        if (assessment.getEndTime() != null && now.isAfter(assessment.getEndTime())) {
            throw new IllegalStateException("This assessment is no longer available");
        }

        List<AssessmentAssignment> assignments = assignmentRepository.findByAssessmentId(assessmentId);
        if (!assignments.isEmpty() && assignments.stream().noneMatch(assignment -> assignment.getUserId().equals(userId))) {
            throw new IllegalStateException("You are not assigned to this assessment");
        }
        assignments.stream().filter(assignment -> assignment.getUserId().equals(userId) && assignment.getDeadline() != null && now.isAfter(assignment.getDeadline()))
                .findFirst().ifPresent(assignment -> { throw new IllegalStateException("The assessment deadline has passed"); });

        AssessmentSession existing = assessmentSessionRepository.findByAssessmentIdAndCandidateIdAndStatus(
                assessmentId, userId, AssessmentSession.SessionStatus.IN_PROGRESS).orElse(null);
        if (existing != null) {
            if (!assessment.isAllowResume()) {
                throw new IllegalStateException("This assessment does not allow resuming an existing attempt");
            }
            return existing;
        }

        List<Long> questionIds = questionRepository.findByAssessmentId(assessmentId).stream()
                .sorted((left, right) -> Integer.compare(left.getOrderIndex(), right.getOrderIndex()))
                .map(AssessmentQuestion::getQuestionBankId).toList();
        if (questionIds.isEmpty()) {
            throw new IllegalStateException("This assessment has no questions");
        }
        if (assessment.isShuffleQuestions()) {
            questionIds = new java.util.ArrayList<>(questionIds);
            Collections.shuffle(questionIds);
        }
        User candidate = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Candidate not found"));
        AssessmentSession session = AssessmentSession.builder()
                .assessmentId(assessmentId)
                .candidate(candidate)
                .durationMinutes(assessment.getDurationMinutes())
                .status(AssessmentSession.SessionStatus.IN_PROGRESS)
                .questionIdsSnapshot(questionIds)
                .build();
        return assessmentSessionRepository.save(session);
    }

    @Override
    @Transactional
    public AssessmentResultDTO submit(Long assessmentId, Long userId, SubmissionDTO submissionDto) {
        AssessmentSession session = assessmentSessionRepository
                .findByAssessmentIdAndCandidateIdAndStatus(assessmentId, userId, AssessmentSession.SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new IllegalStateException("No active assessment session found"));

        // A final answer may be sent with submit; all earlier answers are already
        // persisted through the autosave endpoint.
        if (submissionDto != null && submissionDto.getQuestionId() != null && submissionDto.getAnswerText() != null) {
            if (!session.getQuestionIdsSnapshot().contains(submissionDto.getQuestionId())) {
                throw new IllegalArgumentException("Question does not belong to this assessment session");
            }
            AssessmentAnswer answer = answerRepository.findBySessionIdAndQuestionId(session.getId(), submissionDto.getQuestionId())
                    .orElseGet(AssessmentAnswer::new);
            answer.setSessionId(session.getId());
            answer.setQuestionId(submissionDto.getQuestionId());
            answer.setSelectedAnswer(submissionDto.getAnswerText());
            answer.setUpdatedAt(LocalDateTime.now());
            answerRepository.save(answer);
        }

        session.setStatus(AssessmentSession.SessionStatus.SUBMITTED);
        session.setSubmittedAt(java.time.OffsetDateTime.now());
        AssessmentSession savedSession = assessmentSessionRepository.save(session);

        // Written and file answers require a human evaluator after the candidate submits.
        for (AssessmentAnswer answer : answerRepository.findBySessionId(savedSession.getId())) {
            Question question = questionBankRepository.findById(answer.getQuestionId()).orElse(null);
            if (question != null && ("WRITTEN".equals(question.getQuestionType()) || "SUBJECTIVE".equals(question.getQuestionType()) || "FILE_UPLOAD".equals(question.getQuestionType()))) {
                answer.setEvaluationStatus("PENDING_EVALUATION");
                answerRepository.save(answer);
            }
        }

        double score = mcqEvaluationService.evaluateSessionMcqs(savedSession);
        double totalScore = questionRepository.findByAssessmentId(assessmentId).stream()
                .map(AssessmentQuestion::getMaxScore)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        return AssessmentResultDTO.builder()
                .sessionId(savedSession.getId())
                .userId(userId)
                .assessmentId(assessmentId)
                .score(score)
                .totalScore(totalScore)
                .status("SUBMITTED")
                .build();
    }
}
