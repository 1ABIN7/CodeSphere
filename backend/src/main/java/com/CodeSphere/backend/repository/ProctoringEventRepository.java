package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.ProctoringEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProctoringEventRepository extends JpaRepository<ProctoringEvent, Long> {
    List<ProctoringEvent> findBySessionIdOrderByTimestampDesc(Long sessionId);
    List<ProctoringEvent> findTop50ByOrderByTimestampDesc();
}
