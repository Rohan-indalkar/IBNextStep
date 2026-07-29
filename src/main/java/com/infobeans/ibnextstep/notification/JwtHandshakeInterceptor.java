package com.infobeans.ibnextstep.notification;


import com.infobeans.ibnextstep.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Runs once, on the initial HTTP handshake before the socket upgrades.
 * The browser can't send an Authorization header on a WebSocket/SockJS
 * handshake, so the JWT is passed as a query param instead:
 *
 *   new SockJS(`${API_BASE}/ws?token=${jwt}`)
 *
 * This endpoint is whitelisted in SecurityConfig (see PUBLIC_ENDPOINTS),
 * so auth here — not the JwtAuthFilter — is what protects it.
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String email = jwtUtil.extractSubject(token);
            if (email == null || jwtUtil.isTokenExpired(token)) {
                response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return false;
            }
            // Picked up by UserHandshakeHandler to build the session Principal.
            attributes.put("email", email);
            return true;
        } catch (Exception e) {
            response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}