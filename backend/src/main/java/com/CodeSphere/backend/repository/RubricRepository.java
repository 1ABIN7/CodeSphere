package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.Rubric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RubricRepository extends JpaRepository<Rubric, Long> {
    Optional<Rubric> findByQuestionId(Long questionId);
}
