package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {

    /**
     * Retrieves all assessment sessions matching a specific execution status state.
     */
    List<AssessmentSession> findByStatus(AssessmentSession.SessionStatus status);
}