package com.codesphere.backend.repository;

import com.codesphere.backend.model.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {
    List<AssessmentSession> findByStatus(AssessmentSession.SessionStatus status);
}