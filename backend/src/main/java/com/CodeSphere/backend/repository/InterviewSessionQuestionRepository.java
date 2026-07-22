package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.interview.InterviewSessionQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewSessionQuestionRepository extends JpaRepository<InterviewSessionQuestion, Long> {
    List<InterviewSessionQuestion> findBySessionIdOrderByOrderIndexAsc(Long sessionId);
}
