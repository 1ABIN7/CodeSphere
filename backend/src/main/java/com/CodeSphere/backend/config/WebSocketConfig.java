package com.CodeSphere.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker destinations
        config.enableSimpleBroker("/topic", "/queue");
        // Prefix for client-to-server destination mappings
        config.setApplicationDestinationPrefixes("/app");
        // User destination prefix for private queues
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP endpoint for frontend client connection
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000")
                .withSockJS();
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:3000");
    }
}
