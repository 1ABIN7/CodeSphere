package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.model.Rubric;
import com.CodeSphere.backend.model.RubricCriterion;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.repository.RubricRepository;
import com.CodeSphere.backend.service.RubricService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class RubricServiceImpl implements RubricService {

    private final RubricRepository rubricRepository;
    private final QuestionBankRepository questionRepository;

    @Override
    public Rubric defineRubric(Long questionId, List<RubricCriterion> criteriaInput) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + questionId));

        // If a rubric already exists for this question, overwrite/update it cleanly
        Rubric rubric = rubricRepository.findByQuestionId(questionId).orElse(new Rubric());
        rubric.setQuestion(question);
        rubric.getCriteria().clear();

        for (RubricCriterion criterion : criteriaInput) {
            rubric.addCriterion(criterion);
        }

        return rubricRepository.save(rubric);
    }

    @Override
    @Transactional(readOnly = true)
    public Rubric getRubricByQuestionId(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new IllegalArgumentException("Question not found with id: " + questionId);
        }
        return rubricRepository.findByQuestionId(questionId)
                .orElseThrow(() -> new IllegalArgumentException("No rubric defined for question id: " + questionId));
    }
}
