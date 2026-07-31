package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.dto.ai.CodingAssistantRequest;
import com.CodeSphere.backend.dto.ai.CodingAssistantResponse;
import com.CodeSphere.backend.service.CodingAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coding-help")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CANDIDATE')")
public class CodingAssistantController {
    private final CodingAssistantService codingAssistantService;

    @PostMapping
    public ResponseEntity<CodingAssistantResponse> chat(@Valid @RequestBody CodingAssistantRequest request) {
        return ResponseEntity.ok(new CodingAssistantResponse(codingAssistantService.answer(request.message())));
    }
}
