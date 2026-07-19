package com.CodeSphere.backend.messaging;

import com.CodeSphere.backend.config.RabbitMQConfig;
import com.CodeSphere.backend.dto.SubmissionDto;
import com.CodeSphere.backend.dto.JudgeRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubmissionProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Sends standard submission data to the evaluation queue.
     */
    public void sendSubmissionMessage(SubmissionDto submissionDto) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_EVALUATION,
                RabbitMQConfig.ROUTING_KEY_EVALUATION,
                submissionDto
        );
    }

    /**
     * Alias method required by ProblemSubmissionController to push evaluation tasks.
     */
    public void pushToQueue(JudgeRequest judgeRequest) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_EVALUATION,
                RabbitMQConfig.ROUTING_KEY_EVALUATION,
                judgeRequest
        );
    }
}