package com.CodeSphere.backend.messaging;

import com.CodeSphere.backend.config.RabbitMQConfig;
import com.CodeSphere.backend.dto.JudgeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubmissionProducer {

    private final RabbitTemplate rabbitTemplate;

    public void pushToQueue(JudgeRequest request) {
        log.info("Pushing submission/run request {} to judge-queue", request.getSubmissionId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                request
        );
    }
}
