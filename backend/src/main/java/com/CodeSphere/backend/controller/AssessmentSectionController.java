package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.AssessmentSection;
import com.CodeSphere.backend.service.AssessmentSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentSectionController {

    private final AssessmentSectionService sectionService;

    @PostMapping("/{assessmentId}/sections")
    public ResponseEntity<AssessmentSection> addSection(
            @PathVariable Long assessmentId,
            @RequestBody AssessmentSection section) {
        section.setAssessmentId(assessmentId);
        return ResponseEntity.ok(sectionService.addSection(section));
    }

    @GetMapping("/{assessmentId}/sections")
    public ResponseEntity<List<AssessmentSection>> getSections(@PathVariable Long assessmentId) {
        return ResponseEntity.ok(sectionService.getSectionsByAssessmentId(assessmentId));
    }

    @PutMapping("/{assessmentId}/sections/reorder")
    public ResponseEntity<Void> reorderSections(
            @PathVariable Long assessmentId,
            @RequestBody List<Long> sectionIds) {
        sectionService.reorderSections(assessmentId, sectionIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/sections/{id}")
    public ResponseEntity<Void> deleteSection(@PathVariable Long id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }
}
