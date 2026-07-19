package com.CodeSphere.backend.service.impl;

import com.CodeSphere.backend.dto.AssessmentResultDto;
import com.CodeSphere.backend.dto.SubmissionDto;
import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import com.CodeSphere.backend.service.AssessmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssessmentServiceImpl implements AssessmentService {

    // --- YOUR UNIQUE MODULE RUNTIME IMPLEMENTATIONS ---

    @Override
    @Transactional(readOnly = true)
    public Assessment getAssessmentById(Long id) {
        // Aligns with your original 'findById' mock framework approach
        System.out.println("Finding assessment details for ID: " + id);
        return Assessment.builder().id(id).title("Mock Dynamic Title Context").build();
    }

    @Override
    @Transactional
    public Object createBlueprint(Object assessmentDto) {
        // TODO: Map Dto to Entity and run assessmentRepository.save(entity)
        return assessmentDto;
    }

    @Override
    @Transactional
    public void start(Long assessmentId, Long userId) {
        // 1. Check if the assessment exists
        // 2. Verify if the user already has an active, unfinished session
        // 3. Initialize and save a new AssessmentSession entity with Instant.now() as start time
        System.out.println("Starting assessment " + assessmentId + " for user " + userId);
    }

    @Override
    @Transactional
    public AssessmentResultDto submit(Long assessmentId, Long userId, SubmissionDto submissionDto) {
        // 1. Fetch the active AssessmentSession for this user and test
        // 2. Loop through submitted answers and compare them against the solution key
        // 3. Calculate total score and update session status to 'COMPLETED'
        // 4. Save metrics and return the result Dto
        System.out.println("Submitting assessment " + assessmentId + " for user " + userId);

        AssessmentResultDto mockResult = new AssessmentResultDto();
        mockResult.setAssessmentId(assessmentId);
        mockResult.setScore(100.0);
        mockResult.setStatus("COMPLETED");

        return mockResult;
    }

    // --- TEAMMATES ORIGINAL MANAGEMENT IMPLEMENTATIONS (STUBS) ---

    @Override
    @Transactional
    public Assessment createAssessment(Assessment assessment) {
        System.out.println("Saving a new assessment entity configuration layout.");
        return assessment;
    }

    @Override
    @Transactional
    public Assessment updateAssessment(Long id, Assessment assessmentDetails) {
        System.out.println("Updating assessment target runtime context ID: " + id);
        return assessmentDetails;
    }

    @Override
    @Transactional
    public void deleteAssessment(Long id) {
        System.out.println("Permanently dropped assessment node matching ID: " + id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assessment> getAllAssessments() {
        return new ArrayList<>();
    }

    @Override
    @Transactional
    public Assessment cloneAssessment(Long id) {
        System.out.println("Duplicating template structure mapping out from target ID: " + id);
        return new Assessment();
    }

    @Override
    @Transactional
    public Assessment publishAssessment(Long id) {
        System.out.println("Publishing target assessment context live matching ID: " + id);
        return new Assessment();
    }

    @Override
    @Transactional
    public Assessment unpublishAssessment(Long id) {
        System.out.println("Withdrawing public availability window context from ID: " + id);
        return new Assessment();
    }

    @Override
    @Transactional
    public AssessmentAssignment assignAssessment(Long id, Long userId, LocalDateTime deadline) {
        System.out.println("Generating assignment dispatch map record linking assessment " + id + " to candidate user " + userId);
        return new AssessmentAssignment();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssessmentAssignment> getAssignedCandidates(Long id) {
        return new ArrayList<>();
    }
}