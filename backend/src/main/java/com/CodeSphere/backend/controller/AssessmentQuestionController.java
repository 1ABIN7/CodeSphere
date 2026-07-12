package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.entity.AssessmentQuestion;
import com.CodeSphere.backend.service.AssessmentQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sections")
@RequiredArgsConstructor
public class AssessmentQuestionController {

    private final AssessmentQuestionService questionService;

    @PostMapping("/{sectionId}/questions")
    public ResponseEntity<AssessmentQuestion> addQuestion(
            @PathVariable Long sectionId,
            @RequestParam Long assessmentId,
            @RequestBody AssessmentQuestion aq) {
        aq.setSectionId(sectionId);
        aq.setAssessmentId(assessmentId);
        return ResponseEntity.ok(questionService.addQuestionToSection(aq));
    }

    @GetMapping("/{sectionId}/questions")
    public ResponseEntity<List<AssessmentQuestion>> getQuestions(@PathVariable Long sectionId) {
        return ResponseEntity.ok(questionService.getQuestionsBySectionId(sectionId));
    }

    @PutMapping("/{sectionId}/questions/reorder")
    public ResponseEntity<Void> reorderQuestions(
            @PathVariable Long sectionId,
            @RequestBody List<Long> mappingIds) {
        questionService.reorderQuestionsInSection(sectionId, mappingIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/questions/{id}")
    public ResponseEntity<Void> removeQuestion(@PathVariable Long id) {
        questionService.removeQuestionFromSection(id);
        return ResponseEntity.noContent().build();
    }
}
