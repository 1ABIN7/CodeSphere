package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.ProctoringConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProctoringConfigRepository extends JpaRepository<ProctoringConfig, Long> {
    Optional<ProctoringConfig> findByAssessmentId(Long assessmentId);
}
