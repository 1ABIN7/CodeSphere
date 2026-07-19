package com.CodeSphere.backend.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // These constants fix your "cannot find symbol" compilation errors
    public static final String QUEUE = "submission.queue";
    public static final String EXCHANGE_EVALUATION = "evaluation.exchange";
    public static final String ROUTING_KEY_EVALUATION = "evaluation.routingKey";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE, true); // true = durable queue
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_EVALUATION);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY_EVALUATION);
    }

    // Converts your Java DTOs into JSON automatically when sending/receiving messages
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}