package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.entity.Assessment;
import com.CodeSphere.backend.entity.AssessmentAssignment;
import com.CodeSphere.backend.service.AssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    public ResponseEntity<Assessment> createAssessment(@RequestBody Assessment assessment) {
        return ResponseEntity.ok(assessmentService.createAssessment(assessment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Assessment> updateAssessment(@PathVariable Long id, @RequestBody Assessment assessment) {
        return ResponseEntity.ok(assessmentService.updateAssessment(id, assessment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssessment(@PathVariable Long id) {
        assessmentService.deleteAssessment(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assessment> getAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssessmentById(id));
    }

    @GetMapping
    public ResponseEntity<List<Assessment>> getAllAssessments() {
        return ResponseEntity.ok(assessmentService.getAllAssessments());
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<Assessment> cloneAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.cloneAssessment(id));
    }

    @PutMapping("/{id}/publish")
    public ResponseEntity<Assessment> publishAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.publishAssessment(id));
    }

    @PutMapping("/{id}/unpublish")
    public ResponseEntity<Assessment> unpublishAssessment(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.unpublishAssessment(id));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<AssessmentAssignment> assignAssessment(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline) {
        return ResponseEntity.ok(assessmentService.assignAssessment(id, userId, deadline));
    }

    @GetMapping("/{id}/assigned-candidates")
    public ResponseEntity<List<AssessmentAssignment>> getAssignedCandidates(@PathVariable Long id) {
        return ResponseEntity.ok(assessmentService.getAssignedCandidates(id));
    }
}
