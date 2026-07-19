package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.model.AssessmentSession;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.CodeSphere.backend.repository.QuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class McqEvaluationService {

    private final AssessmentAnswerRepository answerRepository;
    private final QuestionRepository questionRepository;

    public McqEvaluationService(AssessmentAnswerRepository answerRepository, QuestionRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Evaluates all MCQ questions within a completed session and computes the total score.
     * Changed return type to double to prevent lossy conversion compilation errors.
     */
    public double evaluateSessionMcqs(AssessmentSession session) {
        double totalScore = 0.0;
        List<Long> questionIds = session.getQuestionIdsSnapshot();

        for (Long qId : questionIds) {
            Question question = questionRepository.findById(qId).orElse(null);
            if (question == null) continue;

            // Only process auto-gradable MCQ types in this service engine
            if ("MCQ_SINGLE".equals(question.getQuestionType()) || "MCQ_MULTI".equals(question.getQuestionType())) {

                AssessmentAnswer answer = answerRepository.findBySessionIdAndQuestionId(session.getId(), qId)
                        .orElse(null);

                totalScore += calculateQuestionScore(question, answer);
            }
        }
        return totalScore;
    }

    /**
     * Internal grading engine helper. Handles fractional value adjustments natively.
     */
    private double calculateQuestionScore(Question question, AssessmentAnswer answer) {
        // If the user skipped the question, they get 0 points (no negative penalty applied)
        if (answer == null || answer.getSelectedAnswer() == null || answer.getSelectedAnswer().trim().isEmpty()) {
            return 0.0;
        }

        String studentInput = answer.getSelectedAnswer().trim();

        // Safe extraction strategy for the correct answers list to prevent List object trimming errors
        String correctInput = "";
        if (question.getCorrectAnswers() != null && !question.getCorrectAnswers().isEmpty()) {
            // Handles cases where correct answer strings are joined together or stored in index 0
            correctInput = question.getCorrectAnswers().stream()
                    .collect(Collectors.joining(","))
                    .trim();
        }

        double points = question.getPoints() != null ? question.getPoints() : 0.0;
        double negativeScore = question.getNegativeScore() != null ? question.getNegativeScore() : 0.0;

        if ("MCQ_SINGLE".equals(question.getQuestionType())) {
            if (studentInput.equalsIgnoreCase(correctInput)) {
                return points;
            } else {
                return -negativeScore;
            }
        }

        if ("MCQ_MULTI".equals(question.getQuestionType())) {
            // Parse multi-choice options by split strings (e.g., "A,B" -> Set containing "A", "B")
            Set<String> studentChoices = Arrays.stream(studentInput.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            Set<String> correctChoices = Arrays.stream(correctInput.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());

            if (studentChoices.equals(correctChoices)) {
                return points;
            } else {
                return -negativeScore;
            }
        }

        return 0.0;
    }
}