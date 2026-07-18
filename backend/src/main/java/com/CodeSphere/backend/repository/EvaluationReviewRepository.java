package com.CodeSphere.backend.repository;

import com.CodeSphere.backend.model.EvaluationReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EvaluationReviewRepository extends JpaRepository<EvaluationReview, Long> {
    List<EvaluationReview> findByAnswerId(Long answerId);
}
