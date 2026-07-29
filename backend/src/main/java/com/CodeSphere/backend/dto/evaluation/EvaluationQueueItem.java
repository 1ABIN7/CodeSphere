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
    String candidateName;
    String answerText;
    String fileUrl;
    Integer maxScore;
    List<RubricCriterionItem> rubricCriteria;
    int reviewCount;
    int requiredReviewCount;

    @Value
    @Builder
    public static class RubricCriterionItem {
        String name;
        Integer maxPoints;
    }
}
