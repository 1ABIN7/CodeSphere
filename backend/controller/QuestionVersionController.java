package com.codesphere.backend.controller;

import com.codesphere.backend.model.Question;
import com.codesphere.backend.model.QuestionVersion;
import com.codesphere.backend.service.QuestionVersionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/questions/{id}")
@CrossOrigin(origins = "*")
public class QuestionVersionController {

    private final QuestionVersionService versionService;

    public QuestionVersionController(QuestionVersionService versionService) {
        this.versionService = versionService;
    }

    // LIST HISTORY: GET /api/v1/questions/{id}/versions
    @GetMapping("/versions")
    public ResponseEntity<List<QuestionVersion>> getVersionHistory(@PathVariable Long id) {
        List<QuestionVersion> history = versionService.getVersionHistory(id);
        return ResponseEntity.ok(history);
    }

    // RESTORE STATE: POST /api/v1/questions/{id}/restore/{version}
    @PostMapping("/restore/{version}")
    public ResponseEntity<Question> restoreToVersion(
            @PathVariable Long id,
            @PathVariable("version") Integer versionNumber) {
        Question restoredQuestion = versionService.restoreToVersion(id, versionNumber);
        return ResponseEntity.ok(restoredQuestion);
    }
}