package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Rubric;
import com.CodeSphere.backend.model.RubricCriterion;

import java.util.List;

public interface RubricService {

    Rubric defineRubric(Long questionId, List<RubricCriterion> criteriaInput);

    Rubric getRubricByQuestionId(Long questionId);
}