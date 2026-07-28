package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.QuestionCategory;
import com.CodeSphere.backend.service.QuestionCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final QuestionCategoryService service;

    public CategoryController(QuestionCategoryService service) {
        this.service = service;
    }

    // CREATE: Post a category. Pass optional ?parentId query param to make it a child
    @PostMapping
    public ResponseEntity<QuestionCategory> createCategory(
            @Valid @RequestBody QuestionCategory category,
            @RequestParam(required = false) Long parentId) {
        QuestionCategory savedCategory = service.createCategory(category, parentId);
        return new ResponseEntity<>(savedCategory, HttpStatus.CREATED);
    }

    // READ TREE: Get all top-level categories along with their nested child objects
    @GetMapping
    public ResponseEntity<List<QuestionCategory>> getCategoryTree() {
        return ResponseEntity.ok(service.getRootCategories());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}