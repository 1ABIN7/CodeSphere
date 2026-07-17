package com.codesphere.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_EVALUATION = "evaluation-queue";
    public static final String EXCHANGE_EVALUATION = "evaluation-exchange";
    public static final String ROUTING_KEY_EVALUATION = "evaluation.written";

    @Bean
    public Queue evaluationQueue() {
        return new Queue(QUEUE_EVALUATION, true);
    }

    @Bean
    public TopicExchange evaluationExchange() {
        return new TopicExchange(EXCHANGE_EVALUATION);
    }

    @Bean
    public Binding binding(Queue evaluationQueue, TopicExchange evaluationExchange) {
        return BindingBuilder.bind(evaluationQueue).to(evaluationExchange).with(ROUTING_KEY_EVALUATION);
    }
}