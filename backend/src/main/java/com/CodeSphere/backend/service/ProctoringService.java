package com.CodeSphere.backend.service;

import com.CodeSphere.backend.model.ProctoringEvent;
import java.util.Map;

public interface ProctoringService {
    ProctoringEvent recordEvent(Long sessionId, ProctoringEvent event);
    Map<String, Object> getProctoringReport(Long sessionId);
}
