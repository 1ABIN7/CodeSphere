package com.CodeSphere.backend.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProctoringEventRequest {
    private String eventType; // e.g. TAB_SWITCH, FACE_MISSING, MULTIPLE_FACES, NOISE_DETECTED
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private Map<String, Object> metadata;
}
