package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.AssessmentAnswer;
import org.springframework.web.multipart.MultipartFile;

public interface FileUploadAssessmentService {

    AssessmentAnswer uploadAndQueueFile(Long sessionId, Long questionId, MultipartFile file);
}