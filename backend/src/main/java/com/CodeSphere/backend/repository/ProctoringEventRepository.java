package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.ProctoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProctoringEventRepository extends JpaRepository<ProctoringEvent, Long> {
    List<ProctoringEvent> findBySessionIdOrderByOccurredAtAsc(Long sessionId);
    long countBySessionId(Long sessionId);
    long countBySessionIdAndSeverity(Long sessionId, String severity);
}
