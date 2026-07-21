package com.codesphere.backend.service;

import com.codesphere.backend.model.Question;
import com.codesphere.backend.model.Rubric;
import com.codesphere.backend.model.RubricCriterion;
import com.codesphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.RubricRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class RubricService {

    private final RubricRepository rubricRepository;
    private final QuestionBankRepository questionRepository;

    public RubricService(RubricRepository rubricRepository, QuestionBankRepository questionRepository) {
        this.rubricRepository = rubricRepository;
        this.questionRepository = questionRepository;
    }

    public Rubric defineRubric(Long questionId, List<RubricCriterion> criteriaInput) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + questionId));

        // If a rubric already exists for this question, we overwrite/update it cleanly
        Rubric rubric = rubricRepository.findById(questionId).orElse(new Rubric());
        rubric.setQuestion(question);
        rubric.getCriteria().clear();

        for (RubricCriterion criterion : criteriaInput) {
            rubric.addCriterion(criterion);
        }

        return rubricRepository.save(rubric);
    }

    @Transactional(readOnly = true)
    public Rubric getRubricByQuestionId(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new IllegalArgumentException("Question not found with id: " + questionId);
        }
        return rubricRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("No rubric defined for question id: " + questionId));
    }
}