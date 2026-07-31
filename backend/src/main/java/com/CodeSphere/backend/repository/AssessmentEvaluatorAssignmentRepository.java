package com.CodeSphere.backend.repository;
import com.CodeSphere.backend.model.AssessmentEvaluatorAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AssessmentEvaluatorAssignmentRepository extends JpaRepository<AssessmentEvaluatorAssignment, Long> {
    List<AssessmentEvaluatorAssignment> findByAnswerId(Long answerId);
    boolean existsByAnswerIdAndEvaluatorId(Long answerId, Long evaluatorId);
    void deleteByAnswerId(Long answerId);
}
