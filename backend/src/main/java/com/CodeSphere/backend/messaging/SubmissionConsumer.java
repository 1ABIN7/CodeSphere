package com.CodeSphere.backend.messaging;

import com.CodeSphere.backend.config.RabbitMQConfig;
import com.CodeSphere.backend.dto.DockerExecutionResult;
import com.CodeSphere.backend.dto.JudgeRequest;
import com.CodeSphere.backend.model.Submission;
import com.CodeSphere.backend.model.SubmissionStatus;
import com.CodeSphere.backend.repository.SubmissionRepository;
import com.CodeSphere.backend.service.DockerExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionConsumer {

    private final DockerExecutionService dockerExecutionService;
    private final SubmissionRepository submissionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void consumeMessage(JudgeRequest request) {
        log.info("Received judge request for submission ID: {}, runOnly: {}", 
                request.getSubmissionId(), request.isRunOnly());

        if (request.isRunOnly()) {
            // Process run code request (run mock tests without saving database submission state)
            DockerExecutionResult result = dockerExecutionService.runCode(
                    request.getCode(),
                    request.getLanguage(),
                    request.getProblemId()
            );
            // Push directly to WebSocket
            messagingTemplate.convertAndSend("/queue/submission-result/" + request.getSubmissionId(), result);
            log.info("Finished processing run request for ID: {}", request.getSubmissionId());
            return;
        }

        // 1. Update database status to RUNNING
        Submission submission = submissionRepository.findById(request.getSubmissionId()).orElse(null);
        if (submission == null) {
            log.error("Submission with ID {} not found in database", request.getSubmissionId());
            return;
        }
        submission.setStatus(SubmissionStatus.RUNNING);
        submissionRepository.save(submission);

        // 2. Execute code in Docker container (via service interface)
        DockerExecutionResult result = dockerExecutionService.executeSubmission(
                submission.getId(),
                submission.getCode(),
                submission.getLanguage(),
                submission.getProblemId()
        );

        // 3. Save outcome back to database
        submission.setStatus(SubmissionStatus.valueOf(result.getVerdict()));
        submission.setExecTime(result.getExecTime());
        submission.setExecMemory(result.getExecMemory());
        submission.setErrorMessage(result.getErrorMessage());
        submissionRepository.save(submission);

        // 4. WebSocket Push final result to user client
        messagingTemplate.convertAndSend("/queue/submission-result/" + submission.getId(), result);
        log.info("Finished judging submission ID: {} with verdict: {}", submission.getId(), result.getVerdict());
    }
}
