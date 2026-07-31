package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.ApiRunResponse;
import com.CodeSphere.backend.model.Question;

public interface ApiAssessmentRunnerService {
    ApiRunResponse run(Question question, String candidateCode);
}
