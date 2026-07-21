package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.QuestionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionCategoryRepository extends JpaRepository<QuestionCategory, Long> {

    // Finds all top-level categories that don't belong to any parent
    List<QuestionCategory> findByParentIsNull();
}