package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.ComprehensionViewDto;

public interface ReadingComprehensionService {

    ComprehensionViewDto getPassageView(Long sessionId, Long passageQuestionId);

    void processSubQuestionSubmission(Long sessionId, Long subQuestionId, String answerContent);
}