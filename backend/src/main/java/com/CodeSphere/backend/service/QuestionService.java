package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;
import java.util.List;

public interface QuestionService {

    Question createQuestion(Question question);

    List<Question> getFilteredQuestions(String category, String type, String difficulty, List<String> tags, String search);

    Question getQuestionById(Long id);

    Question updateQuestion(Long id, Question questionDetails);

    void deleteQuestion(Long id);

    void updateStatus(Long id, String status);
}