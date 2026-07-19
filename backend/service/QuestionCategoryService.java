package com.codesphere.backend.service;

import com.codesphere.backend.model.QuestionCategory;
import com.codesphere.backend.repository.QuestionCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class QuestionCategoryService {

    private final QuestionCategoryRepository repository;

    public QuestionCategoryService(QuestionCategoryRepository repository) {
        this.repository = repository;
    }

    public QuestionCategory createCategory(QuestionCategory category, Long parentId) {
        if (parentId != null) {
            QuestionCategory parent = repository.findById(parentId)
                    .orElseThrow(() -> new EntityNotFoundException("Parent category not found with id: " + parentId));
            category.setParent(parent);
        }
        return repository.save(category);
    }

    @Transactional(readOnly = true)
    public List<QuestionCategory> getRootCategories() {
        return repository.findByParentIsNull();
    }

    @Transactional(readOnly = true)
    public QuestionCategory getCategoryById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

    public void deleteCategory(Long id) {
        QuestionCategory category = getCategoryById(id);
        repository.delete(category);
    }
}