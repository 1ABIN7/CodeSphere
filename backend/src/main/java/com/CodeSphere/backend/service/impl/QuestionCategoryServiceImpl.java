package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.model.QuestionCategory;
import com.CodeSphere.backend.repository.QuestionCategoryRepository;
import com.CodeSphere.backend.service.QuestionCategoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class QuestionCategoryServiceImpl implements QuestionCategoryService {

    private final QuestionCategoryRepository repository;

    @Override
    public QuestionCategory createCategory(QuestionCategory category, Long parentId) {
        if (parentId != null) {
            QuestionCategory parent = repository.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("Parent category not found with id: " + parentId));
            category.setParent(parent);
        }
        return repository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionCategory> getRootCategories() {
        return repository.findByParentIsNull();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionCategory getCategoryById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

    @Override
    public void deleteCategory(Long id) {
        QuestionCategory category = getCategoryById(id);
        repository.delete(category);
    }
}