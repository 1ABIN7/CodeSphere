package com.CodeSphere.backend.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.util.Map;

@Controller
@Slf4j
public class SessionWebSocketController {

    @MessageMapping("/session/{sessionId}/timer-sync")
    @SendTo("/topic/session-timer/{sessionId}")
    public Map<String, Object> syncSessionTimer(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> timerPayload) {
        log.debug("Received session timer sync for session #{}: {}", sessionId, timerPayload);
        return timerPayload;
    }

    @MessageMapping("/session/{sessionId}/proctor-alert")
    @SendTo("/topic/proctor-events/{sessionId}")
    public Map<String, Object> broadcastProctorAlert(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> alertPayload) {
        log.info("Broadcasting live proctor alert for session #{}: {}", sessionId, alertPayload);
        return alertPayload;
    }
}
