package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.interview.InterviewAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewAttemptRepository extends JpaRepository<InterviewAttempt, Long> {
    List<InterviewAttempt> findBySessionId(Long sessionId);
    Optional<InterviewAttempt> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
}
