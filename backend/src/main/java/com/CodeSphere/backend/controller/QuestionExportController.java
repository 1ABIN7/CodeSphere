package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.Question;
import com.CodeSphere.backend.repository.QuestionBankRepository;
import com.CodeSphere.backend.service.QuestionExportService;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@CrossOrigin(origins = "*")
public class QuestionExportController {

    private final QuestionBankRepository repository;
    private final QuestionExportService exportService;

    public QuestionExportController(QuestionBankRepository repository, QuestionExportService exportService) {
        this.repository = repository;
        this.exportService = exportService;
    }

    // BULK EXPORT: GET /api/v1/questions/export
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportQuestions(
            @RequestParam(value = "format", defaultValue = "json") String format,
            @RequestParam(value = "categoryId", required = false) String categoryId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "difficulty", required = false) String difficulty) {

        // 1. Query database and filter in memory
        List<Question> questions = repository.findAll().stream()
                .filter(q -> categoryId == null || categoryId.isBlank() ||
                        (q.getCategory() != null && q.getCategory().equalsIgnoreCase(categoryId)))
                .filter(q -> type == null || type.isBlank() ||
                        (q.getType() != null && q.getType().equalsIgnoreCase(type)))
                .filter(q -> difficulty == null || difficulty.isBlank() ||
                        (q.getDifficulty() != null && q.getDifficulty().equalsIgnoreCase(difficulty)))
                .collect(Collectors.toList());

        // 2. Format database results into output bytes
        byte[] data = exportService.exportQuestions(questions, format);

        // 3. Set content type disposition headers
        String fileName = "questions_export_" + System.currentTimeMillis() + "." + format.toLowerCase();
        MediaType mediaType = "csv".equalsIgnoreCase(format)
                ? MediaType.parseMediaType("text/csv")
                : MediaType.APPLICATION_JSON;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(mediaType)
                .body(data);
    }
}