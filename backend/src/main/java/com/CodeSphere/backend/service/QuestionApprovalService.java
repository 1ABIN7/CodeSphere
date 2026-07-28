package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.Question;

public interface QuestionApprovalService {

    Question submitForApproval(Long id);

    Question approveQuestion(Long id);

    Question rejectQuestion(Long id, String feedback);
}