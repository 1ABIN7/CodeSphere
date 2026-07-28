package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;
import java.util.List;

public interface QuestionService {

    List<Question> getAllQuestions();

    Question getQuestionById(Long id);

    Question createQuestion(Question question);

    Question updateQuestion(Long id, Question question);

    void deleteQuestion(Long id);

    // --- Added methods for QuestionController and QuestionBankController ---
    void updateStatus(Long id, String status);

    List<Question> getFilteredQuestions(String category, String difficulty, String status, List<String> tags, String search);
}