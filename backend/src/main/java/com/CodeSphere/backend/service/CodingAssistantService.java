package com.CodeSphere.backend.service;

public interface CodingAssistantService {
    String answer(String message);
    String evaluateCandidateSkills(String candidateName, String submissionSummary);
}
