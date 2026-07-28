package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.Question; // Adjust package based on project structure
import com.CodeSphere.backend.service.QuestionService; // Adjust package based on project structure
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@CrossOrigin(origins = "*") // Adjust or remove based on security/CORS policy
public class QuestionBankController {

    private final QuestionService questionService;

    // Constructor injection
    public QuestionBankController(QuestionService questionService) {
        this.questionService = questionService;
    }

    // CREATE: POST /api/v1/questions
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    public ResponseEntity<Question> createQuestion(@Valid @RequestBody Question question) {
        Question createdQuestion = questionService.createQuestion(question);
        return new ResponseEntity<>(createdQuestion, HttpStatus.CREATED);
    }

    // READ ALL: GET /api/v1/questions
// READ ALL with Filtering & Search: GET /api/v1/questions
    @GetMapping
    public ResponseEntity<List<Question>> getAllQuestions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String search) {

        List<Question> questions = questionService.getFilteredQuestions(category, type, difficulty, tags, search);
        return ResponseEntity.ok(questions);
    }

    // READ ONE: GET /api/v1/questions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Question> getQuestionById(@PathVariable Long id) {
        Question question = questionService.getQuestionById(id);
        return ResponseEntity.ok(question);
    }

    // UPDATE: PUT /api/v1/questions/{id}
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    public ResponseEntity<Question> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody Question questionDetails) {
        Question updatedQuestion = questionService.updateQuestion(id, questionDetails);
        return ResponseEntity.ok(updatedQuestion);
    }

    // DELETE: DELETE /api/v1/questions/{id}
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
