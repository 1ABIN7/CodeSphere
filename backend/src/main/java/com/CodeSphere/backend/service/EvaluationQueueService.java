package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.EvaluationReview;
import com.CodeSphere.backend.model.SessionAnswer;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;

public interface EvaluationQueueService {
    Page<SessionAnswer> getPendingEvaluations(Long evaluatorId, int page, int size);
    void submitScore(Long answerId, Double score, String feedback, Long evaluatorId);
    void submitRubricScores(Long answerId, Map<String, Object> rubricScores, String feedback, Long evaluatorId);
    List<EvaluationReview> getReviewsForAnswer(Long answerId);
    void assignPendingAnswer(Long answerId);
}
