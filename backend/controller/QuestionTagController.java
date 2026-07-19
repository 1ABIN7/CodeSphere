package com.codesphere.backend.controller;

import com.codesphere.backend.service.QuestionTagService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/questions/tags")
@CrossOrigin(origins = "*")
public class QuestionTagController {

    private final QuestionTagService tagService;

    public QuestionTagController(QuestionTagService tagService) {
        this.tagService = tagService;
    }

    // GET /api/v1/questions/tags/autocomplete?query=
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam("query") String query) {
        List<String> suggestions = tagService.autocompleteTags(query);
        return ResponseEntity.ok(suggestions);
    }

    // GET /api/v1/questions/tags/trending?limit=10
    @GetMapping("/trending")
    public ResponseEntity<List<Map.Entry<String, Long>>> getTrending(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        List<Map.Entry<String, Long>> trending = tagService.getTrendingTags(limit);
        return ResponseEntity.ok(trending);
    }
}