package com.CodeSphere.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // --- Judge Queue Config ---
    public static final String QUEUE_JUDGE = "judge-queue";
    public static final String EXCHANGE_JUDGE = "judge-exchange";
    public static final String ROUTING_KEY_JUDGE = "judge.routing.key";

    // --- Evaluation Queue Config ---
    public static final String QUEUE_EVALUATION = "evaluation-queue";
    public static final String EXCHANGE_EVALUATION = "evaluation-exchange";
    public static final String ROUTING_KEY_EVALUATION = "evaluation.written";

    // ==========================================
    // 1. JUDGE QUEUE BEANS
    // ==========================================
    @Bean
    public Queue judgeQueue() {
        return new Queue(QUEUE_JUDGE, true);
    }

    @Bean
    public TopicExchange judgeExchange() {
        return new TopicExchange(EXCHANGE_JUDGE);
    }

    @Bean
    public Binding judgeBinding(Queue judgeQueue, TopicExchange judgeExchange) {
        return BindingBuilder.bind(judgeQueue).to(judgeExchange).with(ROUTING_KEY_JUDGE);
    }

    // ==========================================
    // 2. EVALUATION QUEUE BEANS
    // ==========================================
    @Bean
    public Queue evaluationQueue() {
        return new Queue(QUEUE_EVALUATION, true);
    }

    @Bean
    public TopicExchange evaluationExchange() {
        return new TopicExchange(EXCHANGE_EVALUATION);
    }

    @Bean
    public Binding evaluationBinding(Queue evaluationQueue, TopicExchange evaluationExchange) {
        return BindingBuilder.bind(evaluationQueue).to(evaluationExchange).with(ROUTING_KEY_EVALUATION);
    }

    // ==========================================
    // 3. GLOBAL RABBITMQ CONVERTER & TEMPLATE
    // ==========================================
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}