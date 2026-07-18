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

    public static final String QUEUE = "judge-queue";
    public static final String EXCHANGE = "judge-exchange";
    public static final String ROUTING_KEY = "judge.routing.key";

    public static final String EVAL_QUEUE = "evaluation-queue";
    public static final String EVAL_ROUTING_KEY = "evaluation.routing.key";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE, true); // durable queue
    }

    @Bean
    public Queue evalQueue() {
        return new Queue(EVAL_QUEUE, true); // durable queue
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    @Bean
    public Binding evalBinding(Queue evalQueue, TopicExchange exchange) {
        return BindingBuilder.bind(evalQueue).to(exchange).with(EVAL_ROUTING_KEY);
    }

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
