package com.CodeSphere.backend.dto.evaluation;

import lombok.Builder;
import lombok.Value;
import java.util.List;

@Value
@Builder
public class EvaluationQueueItem {
    Long answerId;
    Long sessionId;
    Long questionId;
    String questionTitle;
    String questionType;
    String questionContent;
    String candidateName;
    String answerText;
    String fileUrl;
    Integer maxScore;
    List<RubricCriterionItem> rubricCriteria;
    int reviewCount;
    int requiredReviewCount;
    List<Long> assignedEvaluatorIds;
    List<ReviewItem> reviews;

    @Value
    @Builder
    public static class RubricCriterionItem {
        String name;
        Integer maxPoints;
    }
    @Value
    @Builder
    public static class ReviewItem { Long evaluatorId; java.math.BigDecimal score; String feedback; }
}
