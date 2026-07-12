package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.model.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Submission entity operations.
 * Supports user-scoped queries, problem-scoped queries, and statistics.
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Page<Submission> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Submission> findByProblemIdAndUserIdOrderByCreatedAtDesc(Long problemId, Long userId, Pageable pageable);

    List<Submission> findByProblemIdAndUserId(Long problemId, Long userId);

    long countByProblemIdAndStatus(Long problemId, SubmissionStatus status);

    long countByProblemId(Long problemId);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(DISTINCT s.problemId) FROM Submission s WHERE s.userId = :userId AND s.status = 'ACCEPTED'")
    long countDistinctAcceptedProblemsByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Submission s WHERE s.problemId = :problemId AND s.userId = :userId AND s.status = 'ACCEPTED' ORDER BY s.createdAt DESC")
    Optional<Submission> findLatestAccepted(@Param("problemId") Long problemId, @Param("userId") Long userId);

    @Query("SELECT DISTINCT s.language FROM Submission s WHERE s.userId = :userId")
    List<String> findDistinctLanguagesByUserId(@Param("userId") Long userId);

    boolean existsByProblemIdAndUserIdAndStatus(Long problemId, Long userId, SubmissionStatus status);
}
