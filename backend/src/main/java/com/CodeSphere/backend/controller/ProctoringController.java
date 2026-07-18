package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.ProctoringEvent;
import com.CodeSphere.backend.service.ProctoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/proctoring")
@RequiredArgsConstructor
public class ProctoringController {

    private final ProctoringService proctoringService;

    @PostMapping("/sessions/{sessionId}/event")
    public ResponseEntity<ProctoringEvent> recordEvent(
            @PathVariable Long sessionId,
            @RequestBody ProctoringEvent event) {
        return ResponseEntity.ok(proctoringService.recordEvent(sessionId, event));
    }

    @GetMapping("/sessions/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getProctoringReport(@PathVariable Long sessionId) {
        return ResponseEntity.ok(proctoringService.getProctoringReport(sessionId));
    }
}
