package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.AssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, Long> {
    Optional<AssessmentAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
    List<AssessmentAnswer> findByEvaluationStatusOrderByUpdatedAtAsc(String evaluationStatus);
    long countByEvaluationStatusIn(java.util.Collection<String> evaluationStatuses);
    List<AssessmentAnswer> findBySessionId(Long sessionId);
    Optional<AssessmentAnswer> findByCodingSubmissionId(Long codingSubmissionId);
}
