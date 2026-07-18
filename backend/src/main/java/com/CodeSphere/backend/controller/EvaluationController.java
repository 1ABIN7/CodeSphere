package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.EvaluationReview;
import com.CodeSphere.backend.model.SessionAnswer;
import com.CodeSphere.backend.service.EvaluationQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/evaluation")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationQueueService evaluationQueueService;

    @GetMapping("/queue")
    public ResponseEntity<Page<SessionAnswer>> getPendingEvaluations(
            @RequestParam Long evaluatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(evaluationQueueService.getPendingEvaluations(evaluatorId, page, size));
    }

    @PostMapping("/answers/{answerId}/score")
    public ResponseEntity<Void> submitScore(
            @PathVariable Long answerId,
            @RequestParam Double score,
            @RequestParam String feedback,
            @RequestParam Long evaluatorId) {
        evaluationQueueService.submitScore(answerId, score, feedback, evaluatorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/answers/{answerId}/rubric")
    public ResponseEntity<Void> submitRubricScores(
            @PathVariable Long answerId,
            @RequestBody Map<String, Object> rubricScores,
            @RequestParam String feedback,
            @RequestParam Long evaluatorId) {
        evaluationQueueService.submitRubricScores(answerId, rubricScores, feedback, evaluatorId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/answers/{answerId}/reviews")
    public ResponseEntity<List<EvaluationReview>> getReviews(@PathVariable Long answerId) {
        return ResponseEntity.ok(evaluationQueueService.getReviewsForAnswer(answerId));
    }
}
