package com.infobeans.ibnextstep.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.infobeans.ibnextstep.notification.JwtHandshakeInterceptor;
import com.infobeans.ibnextstep.notification.UserHandshakeHandler;

/**
 * Enables STOMP-over-WebSocket (with SockJS fallback) for real-time
 * notification delivery.
 *
 * Flow:
 *  - Client connects to /ws?token=<jwt>
 *  - JwtHandshakeInterceptor validates the token and stores the user's
 *    email in the handshake attributes
 *  - UserHandshakeHandler turns that email into the session's Principal
 *  - NotificationService then pushes to a specific user via
 *    messagingTemplate.convertAndSendToUser(email, "/queue/notifications", payload)
 *  - Spring routes that to the subscriber listening on
 *    /user/queue/notifications on the client that authenticated as that email
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker is enough for a single-instance deployment.
        // Swap enableSimpleBroker for enableStompBrokerRelay(...) if you
        // move to RabbitMQ/ActiveMQ behind multiple app instances later.
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new UserHandshakeHandler())
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOriginPatterns("*") // tighten to your frontend origin in prod
                .withSockJS();
    }
}