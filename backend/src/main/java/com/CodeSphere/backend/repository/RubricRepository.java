package com.CodeSphere.backend.repository;

import com.codesphere.backend.model.Rubric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RubricRepository extends JpaRepository<Rubric, Long> {
}