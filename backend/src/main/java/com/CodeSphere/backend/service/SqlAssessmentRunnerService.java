package com.CodeSphere.backend.service;

import com.CodeSphere.backend.dto.SqlRunResponse;
import com.CodeSphere.backend.model.Question;

public interface SqlAssessmentRunnerService {
    SqlRunResponse run(Question question, String candidateSql);
}
