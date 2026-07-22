package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.interview.InterviewPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewPerformanceRepository extends JpaRepository<InterviewPerformance, Long> {
    Optional<InterviewPerformance> findByUserIdAndCategoryId(Long userId, Long categoryId);
    List<InterviewPerformance> findByUserId(Long userId);
    List<InterviewPerformance> findByCategoryIdOrderByBestScoreDescTotalAttemptedDesc(Long categoryId);
}
