package com.CodeSphere.backend.dto.evaluation;

import lombok.Builder;
import lombok.Value;

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
}
