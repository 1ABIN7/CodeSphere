package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.QuestionCategory;

import java.util.List;

public interface QuestionCategoryService {

    QuestionCategory createCategory(QuestionCategory category, Long parentId);

    List<QuestionCategory> getRootCategories();

    QuestionCategory getCategoryById(Long id);

    void deleteCategory(Long id);
}