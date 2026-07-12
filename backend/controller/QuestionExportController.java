package com.codesphere.backend.controller;

import com.codesphere.backend.model.Question;
import com.codesphere.backend.repository.QuestionBankRepository;
import com.codesphere.backend.service.QuestionExportService;
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
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "difficulty", required = false) String difficulty) {

        // 1. Query the database using existing matching logic or Specification layer
        // If repo doesn't have an exact multi-match filter signature, adapt this query method to match yours:
        List<Question> questions = repository.findAll().stream()
                .filter(q -> categoryId == null || (q.getCategory() != null && q.getCategory().getId().equals(categoryId)))
                .filter(q -> type == null || type.isBlank() || q.getType().equalsIgnoreCase(type))
                .filter(q -> difficulty == null || difficulty.isBlank() || q.getDifficulty().equalsIgnoreCase(difficulty))
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