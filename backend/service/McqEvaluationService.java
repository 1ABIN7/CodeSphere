package com.codesphere.backend.service;

import com.codesphere.backend.model.AssessmentAnswer;
import com.codesphere.backend.model.AssessmentSession;
import com.codesphere.backend.model.Question;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import com.codesphere.backend.repository.QuestionBankRepository;
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
    private final QuestionBankRepository questionRepository;

    public McqEvaluationService(AssessmentAnswerRepository answerRepository, QuestionBankRepository questionRepository) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Evaluates all MCQ questions within a completed session and computes the total score.
     */
    public int evaluateSessionMcqs(AssessmentSession session) {
        int totalScore = 0;
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

    private int calculateQuestionScore(Question question, AssessmentAnswer answer) {
        // If the user skipped the question, they get 0 points (no negative penalty applied)
        if (answer == null || answer.getSelectedAnswer() == null || answer.getSelectedAnswer().trim().isEmpty()) {
            return 0;
        }

        String studentInput = answer.getSelectedAnswer().trim();
        String correctInput = question.getCorrectAnswers() != null ? question.getCorrectAnswers().trim() : "";

        if ("MCQ_SINGLE".equals(question.getQuestionType())) {
            if (studentInput.equalsIgnoreCase(correctInput)) {
                return question.getPoints();
            } else {
                return -question.getNegativeScore();
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
                return question.getPoints();
            } else {
                return -question.getNegativeScore();
            }
        }

        return 0;
    }
}