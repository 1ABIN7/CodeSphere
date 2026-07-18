package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.interview.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    List<InterviewSession> findByUserIdOrderByStartedAtDesc(Long userId);
}
