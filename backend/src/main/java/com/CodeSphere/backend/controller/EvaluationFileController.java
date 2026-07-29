package com.CodeSphere.backend.controller;

import com.CodeSphere.backend.model.AssessmentAnswer;
import com.CodeSphere.backend.repository.AssessmentAnswerRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.nio.file.Files;
import java.nio.file.Path;

/** Streams a submitted assessment file only to authorized evaluators. */
@RestController
@RequestMapping("/api/v1/evaluations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ORG_ADMIN', 'EXAMINER')")
public class EvaluationFileController {
    private final AssessmentAnswerRepository answerRepository;
    private final MinioClient minioClient;

    @GetMapping("/{answerId}/file")
    public ResponseEntity<InputStreamResource> download(@PathVariable Long answerId) throws Exception {
        AssessmentAnswer answer = answerRepository.findById(answerId).orElseThrow(() -> new IllegalArgumentException("Submission file not found"));
        if (answer.getFileUrl() == null || answer.getFileUrl().isBlank()) throw new IllegalArgumentException("This answer has no uploaded file");
        if (answer.getFileUrl().startsWith("local://")) {
            String filename = answer.getFileUrl().substring("local://".length());
            if (filename.contains("/") || filename.contains("\\")) throw new IllegalArgumentException("Stored submission file is invalid");
            Path localFile = Path.of(System.getProperty("user.dir"), ".local-assessment-files", filename);
            if (!Files.exists(localFile)) throw new IllegalArgumentException("Stored submission file is unavailable");
            MediaType mediaType = filename.endsWith(".pdf") ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(Files.newInputStream(localFile)));
        }
        String path = answer.getFileUrl().startsWith("/") ? answer.getFileUrl().substring(1) : answer.getFileUrl();
        int separator = path.indexOf('/');
        if (separator < 1 || separator == path.length() - 1) throw new IllegalArgumentException("Stored submission file is invalid");
        String bucket = path.substring(0, separator);
        String object = path.substring(separator + 1);
        String filename = object.substring(object.lastIndexOf('/') + 1);
        MediaType mediaType = filename.endsWith(".pdf") ? MediaType.APPLICATION_PDF : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok().contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename.replace("\"", "") + "\"")
                .body(new InputStreamResource(minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(object).build())));
    }
}
