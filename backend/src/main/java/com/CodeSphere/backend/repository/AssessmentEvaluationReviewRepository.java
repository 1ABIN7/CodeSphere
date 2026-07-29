package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.AssessmentEvaluationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssessmentEvaluationReviewRepository extends JpaRepository<AssessmentEvaluationReview, Long> {
    List<AssessmentEvaluationReview> findByAnswerIdOrderByReviewedAtAsc(Long answerId);
    Optional<AssessmentEvaluationReview> findByAnswerIdAndEvaluatorId(Long answerId, Long evaluatorId);
}
