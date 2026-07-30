package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.service.QuestionImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@CrossOrigin(origins = "*")
public class QuestionImportController {

    private final QuestionImportService importService;

    public QuestionImportController(QuestionImportService importService) {
        this.importService = importService;
    }

    // BULK IMPORT: POST /api/v1/questions/import
    @PostMapping("/import")
    public ResponseEntity<List<Question>> importBulkQuestions(@RequestParam("file") MultipartFile file,
                                                              @RequestParam(defaultValue = "UPSERT") String mode) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        boolean updateExisting = !"CREATE".equalsIgnoreCase(mode);
        List<Question> savedList = importService.importBulkQuestions(file, updateExisting);
        return ResponseEntity.ok(savedList);
    }
}
