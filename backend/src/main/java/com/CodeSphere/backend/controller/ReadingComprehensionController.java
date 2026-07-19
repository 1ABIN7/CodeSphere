package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.ComprehensionViewDto;
import com.CodeSphere.backend.service.ReadingComprehensionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/comprehension")
@CrossOrigin(origins = "*")
public class ReadingComprehensionController {

    private final ReadingComprehensionService comprehensionService;

    public ReadingComprehensionController(ReadingComprehensionService comprehensionService) {
        this.comprehensionService = comprehensionService;
    }

    // GET /api/v1/comprehension/session/{sessionId}/passage/{passageId}
    @GetMapping("/session/{sessionId}/passage/{passageId}")
    public ResponseEntity<ComprehensionViewDto> getPassageViewport(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("passageId") Long passageId) {
        return ResponseEntity.ok(comprehensionService.getPassageView(sessionId, passageId));
    }

    // POST /api/v1/comprehension/session/{sessionId}/sub-questions/{subQuestionId}/submit
    @PostMapping("/session/{sessionId}/sub-questions/{subQuestionId}/submit")
    public ResponseEntity<Void> submitSubQuestionAnswer(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("subQuestionId") Long subQuestionId,
            @RequestBody String answerContent) {
        comprehensionService.processSubQuestionSubmission(sessionId, subQuestionId, answerContent);
        return ResponseEntity.ok().build();
    }
}