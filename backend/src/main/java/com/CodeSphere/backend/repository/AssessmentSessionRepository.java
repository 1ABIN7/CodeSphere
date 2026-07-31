package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.AssessmentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {

    /**
     * Retrieves all assessment sessions matching a given status.
     */
    List<AssessmentSession> findByStatus(AssessmentSession.SessionStatus status);

    long countByStatus(AssessmentSession.SessionStatus status);

    Optional<AssessmentSession> findByAssessmentIdAndCandidateIdAndStatus(
            Long assessmentId, Long candidateId, AssessmentSession.SessionStatus status);

    List<AssessmentSession> findByAssessmentId(Long assessmentId);
    List<AssessmentSession> findByCandidateIdAndStatusOrderBySubmittedAtDesc(Long candidateId, AssessmentSession.SessionStatus status);
    List<AssessmentSession> findByCandidateIdAndStatusInOrderBySubmittedAtDesc(Long candidateId, List<AssessmentSession.SessionStatus> statuses);
    Optional<AssessmentSession> findFirstByAssessmentIdAndCandidateIdAndStatusInOrderBySubmittedAtDesc(Long assessmentId, Long candidateId, List<AssessmentSession.SessionStatus> statuses);
    boolean existsByAssessmentIdAndCandidateIdAndStatusIn(Long assessmentId, Long candidateId, List<AssessmentSession.SessionStatus> statuses);
}
