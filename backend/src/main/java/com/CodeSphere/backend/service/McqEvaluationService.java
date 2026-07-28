package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AssessmentSession;

public interface McqEvaluationService {

    int evaluateSessionMcqs(AssessmentSession session);
}