package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.SubmissionResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for per-test-case SubmissionResult entities.
 */
@Repository
public interface SubmissionResultRepository extends JpaRepository<SubmissionResult, Long> {

    List<SubmissionResult> findBySubmissionIdOrderByTestCaseIdAsc(Long submissionId);

    void deleteBySubmissionId(Long submissionId);
}
