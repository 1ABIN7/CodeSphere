package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TestCase entity operations.
 */
@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    List<TestCase> findByProblemIdOrderByOrderIndexAsc(Long problemId);

    List<TestCase> findByProblemIdAndIsSampleTrueOrderByOrderIndexAsc(Long problemId);

    long countByProblemId(Long problemId);

    void deleteByProblemId(Long problemId);
}
