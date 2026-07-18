package com.codesphere.backend.repository;

import com.codesphere.backend.model.AssessmentAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, Long> {
    Optional<AssessmentAnswer> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
}