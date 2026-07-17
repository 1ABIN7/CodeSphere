package com.codesphere.backend.controller;

import com.codesphere.backend.model.AssessmentAnswer;
import com.codesphere.backend.model.AssessmentSection;
import com.codesphere.backend.model.AssessmentSession;
import com.codesphere.backend.model.Question;
import com.codesphere.backend.service.AssessmentSessionService;
import com.codesphere.backend.service.FileUploadAssessmentService;
import com.codesphere.backend.service.WrittenAssessmentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
@CrossOrigin(origins = "*")
public class AssessmentSessionController {

    private final AssessmentSessionService sessionService;
    private final WrittenAssessmentService writtenAssessmentService;
    private final FileUploadAssessmentService fileUploadAssessmentService;

    public AssessmentSessionController(AssessmentSessionService sessionService,
                                       WrittenAssessmentService writtenAssessmentService,
                                       FileUploadAssessmentService fileUploadAssessmentService) {
        this.sessionService = sessionService;
        this.writtenAssessmentService = writtenAssessmentService;
        this.fileUploadAssessmentService = fileUploadAssessmentService;
    }

    // 1. Start a new exam session
    @PostMapping("/start")
    public ResponseEntity<AssessmentSession> startSession(Principal principal, @RequestParam("duration") int duration) {
        String username = principal != null ? principal.getName() : "anonymous_user";
        return ResponseEntity.ok(sessionService.startSession(username, duration));
    }

    // 2. Standard auto-save (MCQ selections or periodic text syncs)
    @PatchMapping("/{id}/answers/{questionId}")
    public ResponseEntity<AssessmentAnswer> autoSaveAnswer(
            @PathVariable("id") Long sessionId,
            @PathVariable("questionId") Long questionId,
            @RequestBody String answerContent) {
        return ResponseEntity.ok(sessionService.autoSaveAnswer(sessionId, questionId, answerContent));
    }

    // 3. Resume an active in-progress session
    @GetMapping("/{id}/resume")
    public ResponseEntity<AssessmentSession> resumeSession(@PathVariable("id") Long sessionId) {
        return ResponseEntity.ok(sessionService.resumeSession(sessionId));
    }

    // 4. Manually submit the exam session
    @PostMapping("/{id}/submit")
    public ResponseEntity<AssessmentSession> submitSession(@PathVariable("id") Long sessionId) {
        return ResponseEntity.ok(sessionService.submitSession(sessionId));
    }

    // 5. Mixed Assessment Section-based navigation (Enforces FREE/SEQUENTIAL rules and per-section timers)
    @GetMapping("/{id}/navigate-section/{targetIndex}")
    public ResponseEntity<?> handleSectionNavigation(
            @PathVariable("id") Long sessionId,
            @PathVariable("targetIndex") int targetIndex,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        try {
            List<AssessmentSection> structuralSections = sessionService.getAssessmentSectionsForSession(sessionId);
            List<Question> sectionQuestions = sessionService.navigateToSection(sessionId, targetIndex, structuralSections, size);
            return ResponseEntity.ok(sectionQuestions);
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 6. Legacy chunk fallback (Gets standard questions layout unconditionally)
    @GetMapping("/{id}/section/{sectionIndex}")
    public ResponseEntity<List<Question>> getSectionQuestions(
            @PathVariable("id") Long sessionId,
            @PathVariable("sectionIndex") int sectionIndex,
            @RequestParam(value = "size", defaultValue = "5") int size) {
        return ResponseEntity.ok(sessionService.getSectionQuestions(sessionId, sectionIndex, size));
    }

    // 7. Submit a subjective written assessment answer with rich HTML text validation
    @PatchMapping("/{id}/written-answers/{questionId}")
    public ResponseEntity<?> submitWrittenAnswer(
            @PathVariable("id") Long sessionId,
            @PathVariable("questionId") Long questionId,
            @RequestBody String rawHtmlAnswer) {
        try {
            AssessmentAnswer answer = writtenAssessmentService.saveAndQueueWrittenAnswer(sessionId, questionId, rawHtmlAnswer);
            return ResponseEntity.ok(answer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 8. Handle multipart file uploads (PDF, DOCX, PPTX, ZIP <= 50MB) targeting MinIO & RabbitMQ
    @PostMapping(value = "/{id}/answers/{questionId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> handleFileUploadAssessment(
            @PathVariable("id") Long sessionId,
            @PathVariable("questionId") Long questionId,
            @RequestParam("file") MultipartFile file) {
        try {
            AssessmentAnswer result = fileUploadAssessmentService.uploadAndQueueFile(sessionId, questionId, file);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}