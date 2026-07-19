package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.interview.InterviewCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterviewCategoryRepository extends JpaRepository<InterviewCategory, Long> {
    Optional<InterviewCategory> findByName(String name);
}
