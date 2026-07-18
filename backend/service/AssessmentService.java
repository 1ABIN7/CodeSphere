package com.codesphere.backend.service;

import com.codesphere.backend.dto.AssessmentResultDTO;
import com.codesphere.backend.dto.SubmissionDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssessmentService {

    /**
     * Finds an assessment blueprint by its ID.
     */
    public Object findById(Long assessmentId) {
        // TODO: Replace with assessmentRepository.findById(assessmentId)
        // For now, returning a mock object context
        return "Assessment Details for ID: " + assessmentId;
    }

    /**
     * Creates a new assessment configuration blueprint.
     */
    @Transactional
    public Object createBlueprint(Object assessmentDto) {
        // TODO: Map DTO to Entity and run assessmentRepository.save(entity)
        return assessmentDto;
    }

    /**
     * Core domain logic to initialize an active test session attempt.
     */
    @Transactional
    public void start(Long assessmentId, Long userId) {
        // 1. Check if the assessment exists
        // 2. Verify if the user already has an active, unfinished session
        // 3. Initialize and save a new AssessmentSession entity with Instant.now() as start time
        System.out.println("Starting assessment " + assessmentId + " for user " + userId);
    }

    /**
     * Core domain logic to process, evaluate, score, and finalize a test submission.
     */
    @Transactional
    public AssessmentResultDTO submit(Long assessmentId, Long userId, SubmissionDTO submissionDto) {
        // 1. Fetch the active AssessmentSession for this user and test
        // 2. Loop through submitted answers and compare them against the solution key
        // 3. Calculate total score and update session status to 'COMPLETED'
        // 4. Save metrics and return the result DTO
        System.out.println("Submitting assessment " + assessmentId + " for user " + userId);

        // Return a mock result DTO to keep the project compiling smoothly
        AssessmentResultDTO mockResult = new AssessmentResultDTO();
        mockResult.setAssessmentId(assessmentId);
        mockResult.setScore(100.0); // Placeholder score
        mockResult.setStatus("COMPLETED");

        return mockResult;
    }
}