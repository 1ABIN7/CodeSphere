package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AssessmentAnswer;

public interface WrittenAssessmentService {

    AssessmentAnswer saveAndQueueWrittenAnswer(Long sessionId, Long questionId, String rawHtmlAnswer);
}