package com.CodeSphere.backend.messaging;

import com.CodeSphere.backend.config.RabbitMQConfig;
import com.CodeSphere.backend.service.EvaluationQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvaluationQueueListener {

    private final EvaluationQueueService evaluationQueueService;

    @RabbitListener(queues = RabbitMQConfig.EVAL_QUEUE)
    public void receiveEvaluationTask(Map<String, Object> taskPayload) {
        log.info("Received evaluation task payload: {}", taskPayload);
        try {
            if (taskPayload.containsKey("answerId")) {
                Long answerId = ((Number) taskPayload.get("answerId")).longValue();
                evaluationQueueService.assignPendingAnswer(answerId);
                log.info("Successfully queued evaluation for answer ID: {}", answerId);
            } else {
                log.warn("Payload missing 'answerId' field: {}", taskPayload);
            }
        } catch (Exception e) {
            log.error("Failed to process evaluation task from queue: {}", e.getMessage());
        }
    }
}
