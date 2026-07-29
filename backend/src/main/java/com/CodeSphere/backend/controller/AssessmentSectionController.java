package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.AssessmentSection;
import com.CodeSphere.backend.service.AssessmentSectionService;
import com.CodeSphere.backend.entity.AssessmentQuestion;
import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.AssessmentQuestionRepository;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessments")
@RequiredArgsConstructor
public class AssessmentSectionController {

    private final AssessmentSectionService sectionService;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionBankRepository questionBankRepository;

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

    @GetMapping("/{assessmentId}/questions")
    public ResponseEntity<List<Question>> getAssessmentQuestions(@PathVariable Long assessmentId) {
        List<Question> questions = assessmentQuestionRepository.findByAssessmentId(assessmentId).stream()
                .sorted(java.util.Comparator.comparing(AssessmentQuestion::getOrderIndex))
                .map(mapping -> questionBankRepository.findById(mapping.getQuestionBankId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        return ResponseEntity.ok(questions);
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
