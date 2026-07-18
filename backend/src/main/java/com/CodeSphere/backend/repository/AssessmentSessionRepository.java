package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {
    List<AssessmentSession> findByUserId(Long userId);
    List<AssessmentSession> findByAssessmentId(Long assessmentId);
    Optional<AssessmentSession> findByAssessmentIdAndUserId(Long assessmentId, Long userId);
}
