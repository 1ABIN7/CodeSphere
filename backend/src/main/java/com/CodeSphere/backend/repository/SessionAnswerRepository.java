package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.SessionAnswer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SessionAnswerRepository extends JpaRepository<SessionAnswer, Long> {
    List<SessionAnswer> findBySessionId(Long sessionId);
    
    Page<SessionAnswer> findByEvaluatedByAndScoreIsNull(Long evaluatorId, Pageable pageable);
    
    long countByEvaluatedByAndScoreIsNull(Long evaluatorId);

    @Query("SELECT COUNT(sa) FROM SessionAnswer sa WHERE sa.isAutoGraded = false AND sa.evaluatedBy = :evaluatorId AND sa.score IS NULL")
    long countPendingEvaluationsByEvaluatorId(@Param("evaluatorId") Long evaluatorId);
}
