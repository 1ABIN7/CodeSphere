package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {
}
