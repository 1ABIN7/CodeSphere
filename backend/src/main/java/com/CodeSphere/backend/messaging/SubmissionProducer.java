package com.CodeSphere.backend.messaging;

import com.CodeSphere.backend.config.RabbitMQConfig;
import com.CodeSphere.backend.dto.JudgeRequest;
import com.CodeSphere.backend.model.SubmissionStatus;
import com.CodeSphere.backend.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionProducer {

    private final RabbitTemplate rabbitTemplate;
    private final SubmissionConsumer submissionConsumer;
    private final SubmissionRepository submissionRepository;

    public void pushToQueue(JudgeRequest request) {
        log.info("Pushing submission/run request {} to judge-queue", request.getSubmissionId());
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_JUDGE,
                    RabbitMQConfig.ROUTING_KEY_JUDGE,
                    request
            );
        } catch (RuntimeException queueUnavailable) {
            // Local development should remain usable without a separate RabbitMQ
            // container. Production continues to use the asynchronous queue.
            log.warn("Judge queue is unavailable; processing submission {} directly.", request.getSubmissionId());
            try {
                submissionConsumer.consumeMessage(request);
            } catch (RuntimeException judgeUnavailable) {
                log.error("Direct judge fallback failed for submission {}", request.getSubmissionId(), judgeUnavailable);
                if (!request.isRunOnly()) {
                    submissionRepository.findById(request.getSubmissionId()).ifPresent(submission -> {
                        submission.setStatus(SubmissionStatus.RUNTIME_ERROR);
                        submission.setErrorMessage("The local judge is unavailable. Start the judge services and submit again.");
                        submissionRepository.save(submission);
                    });
                }
            }
        }
    }
}
