package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.Rubric;
import com.CodeSphere.backend.model.RubricCriterion;
import com.CodeSphere.backend.service.RubricService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/questions/{id}/rubric")
@CrossOrigin(origins = "*")
public class RubricController {

    private final RubricService rubricService;

    public RubricController(RubricService rubricService) {
        this.rubricService = rubricService;
    }

    // POST /api/v1/questions/{id}/rubric
    @PostMapping
    public ResponseEntity<Rubric> defineRubric(
            @PathVariable("id") Long questionId,
            @RequestBody List<RubricCriterion> criteria) {
        Rubric savedRubric = rubricService.defineRubric(questionId, criteria);
        return ResponseEntity.ok(savedRubric);
    }

    // GET /api/v1/questions/{id}/rubric
    @GetMapping
    public ResponseEntity<Rubric> getRubric(@PathVariable("id") Long questionId) {
        Rubric rubric = rubricService.getRubricByQuestionId(questionId);
        return ResponseEntity.ok(rubric);
    }
}