package com.codesphere.backend.service;

import com.codesphere.backend.model.AssessmentAnswer;
import com.codesphere.backend.model.AssessmentSession;
import com.codesphere.backend.model.ComprehensionViewDto;
import com.codesphere.backend.model.Question;
import com.codesphere.backend.repository.AssessmentSessionRepository;
import com.codesphere.backend.repository.QuestionBankRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional
public class ReadingComprehensionService {

    private final QuestionBankRepository questionRepository;
    private final AssessmentSessionRepository sessionRepository;
    private final McqEvaluationService mcqEvaluationService;
    private final WrittenAssessmentService writtenAssessmentService;

    public ReadingComprehensionService(QuestionBankRepository questionRepository,
                                       AssessmentSessionRepository sessionRepository,
                                       McqEvaluationService mcqEvaluationService,
                                       WrittenAssessmentService writtenAssessmentService) {
        this.questionRepository = questionRepository;
        this.sessionRepository = sessionRepository;
        this.mcqEvaluationService = mcqEvaluationService;
        this.writtenAssessmentService = writtenAssessmentService;
    }

    /**
     * Serves the safe phase viewport for a specific passage based on elapsed session interaction time.
     */
    public ComprehensionViewDto getPassageView(Long sessionId, Long passageQuestionId) {
        AssessmentSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Active session not found"));

        Question passageQuestion = questionRepository.findById(passageQuestionId)
                .orElseThrow(() -> new IllegalArgumentException("Passage question target not found"));

        // Compute elapsed tracking time against the specific passage phase rules
        long secondsElapsed = ChronoUnit.SECONDS.between(session.getStartedAt(), LocalDateTime.now());
        long readingLimit = passageQuestion.getReadingDurationSeconds();

        if (secondsElapsed < readingLimit) {
            // Phase 1: Reading window active. Serve passage ONLY, conceal the sub-questions completely.
            return new ComprehensionViewDto(
                    passageQuestion.getId(),
                    passageQuestion.getPassageText(),
                    (int) (readingLimit - secondsElapsed),
                    "READING",
                    null
            );
        } else {
            // Phase 2: Reading time expired. Unlock the sub-questions payload.
            List<Question> subQuestions = questionRepository.findAll().stream()
                    .filter(q -> passageQuestionId.equals(q.getParentQuestionId()))
                    .toList();

            return new ComprehensionViewDto(
                    passageQuestion.getId(),
                    passageQuestion.getPassageText(),
                    0,
                    "QUESTIONS",
                    subQuestions
            );
        }
    }

    /**
     * Process submissions for a specific comprehension sub-question answer snapshot.
     */
    public void processSubQuestionSubmission(Long sessionId, Long subQuestionId, String answerContent) {
        Question subQuestion = questionRepository.findById(subQuestionId)
                .orElseThrow(() -> new IllegalArgumentException("Sub-question target not found"));

        String type = subQuestion.getQuestionType();

        if ("MCQ_SINGLE".equals(type) || "MCQ_MULTI".equals(type)) {
            // MCQ Routing: Route right to your auto-save snapshot mapping layer
            // (Graded during standard downstream pipeline processing session sweeps)
            System.out.println("Auto-routing MCQ sub-question: " + subQuestionId);
        } else if ("SHORT_ANSWER".equals(type) || "WRITTEN".equals(type)) {
            // Short Answer / Subjective text routing: Send to the RabbitMQ validation and evaluation pipeline
            writtenAssessmentService.saveAndQueueWrittenAnswer(sessionId, subQuestionId, answerContent);
            System.out.println("Queued short-answer sub-question to RabbitMQ: " + subQuestionId);
        }
    }
}