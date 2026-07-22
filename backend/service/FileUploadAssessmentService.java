package com.codesphere.backend.service;

import com.codesphere.backend.config.MinioConfig;
import com.codesphere.backend.config.RabbitMQConfig;
import com.codesphere.backend.model.AssessmentAnswer;
import com.codesphere.backend.repository.AssessmentAnswerRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class FileUploadAssessmentService {

    private final AssessmentAnswerRepository answerRepository;
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    private final RabbitTemplate rabbitTemplate;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB Bound Barrier
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "pptx", "zip");

    public FileUploadAssessmentService(AssessmentAnswerRepository answerRepository,
                                       MinioClient minioClient,
                                       MinioConfig minioConfig,
                                       RabbitTemplate rabbitTemplate) {
        this.answerRepository = answerRepository;
        this.minioClient = minioClient;
        this.minioConfig = minioConfig;
        this.rabbitTemplate = rabbitTemplate;
    }

    public AssessmentAnswer uploadAndQueueFile(Long sessionId, Long questionId, MultipartFile file) {
        // 1. Validate File Size Constraint Limits
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Invalid file payload size. Maximum allowance is 50MB.");
        }

        // 2. Extract and Validate Extension Content Format Types
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file type format. System accepts: PDF, DOCX, PPTX, ZIP.");
        }

        try {
            String bucketName = minioConfig.getBucketName();

            // Ensure target bucket path namespace exists in MinIO instance topology
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            // Generate an isolated file object namespace key path
            String objectKey = String.format("session_%d/q_%d_%s.%s",
                    sessionId, questionId, UUID.randomUUID().toString().substring(0, 8), extension);

            // Stream standard binary components up to object layer storage
            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectKey)
                                .stream(is, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            // Resolve public reference string asset location pointing downstream
            String resolvedFileUrl = String.format("/%s/%s", bucketName, objectKey);

            // 3. Upsert execution answers map table model record
            AssessmentAnswer answer = answerRepository.findBySessionIdAndQuestionId(sessionId, questionId)
                    .orElse(new AssessmentAnswer());

            answer.setSessionId(sessionId);
            answer.setQuestionId(questionId);
            answer.setFileUrl(resolvedFileUrl);
            answer.setEvaluationStatus("PENDING_EVALUATION");

            AssessmentAnswer savedAnswer = answerRepository.save(answer);

            // 4. Fire standard state update transaction notice message event payloads out across RabbitMQ
            String routingMessage = String.format("{\"sessionId\":%d,\"questionId\":%d,\"answerId\":%d,\"fileUrl\":\"%s\"}",
                    sessionId, questionId, savedAnswer.getId(), resolvedFileUrl);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_EVALUATION,
                    RabbitMQConfig.ROUTING_KEY_EVALUATION,
                    routingMessage
            );

            return savedAnswer;

        } catch (Exception e) {
            throw new RuntimeException("Object storage engine initialization processing failure: " + e.getMessage(), e);
        }
    }
}