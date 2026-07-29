package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.entity.AssessmentAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssessmentAssignmentRepository extends JpaRepository<AssessmentAssignment, Long> {
    List<AssessmentAssignment> findByAssessmentId(Long assessmentId);
    List<AssessmentAssignment> findByUserId(Long userId);
    boolean existsByAssessmentIdAndUserId(Long assessmentId, Long userId);
}
