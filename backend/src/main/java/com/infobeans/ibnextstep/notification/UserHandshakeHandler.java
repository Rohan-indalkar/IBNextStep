package com.infobeans.ibnextstep.notification;


import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

/**
 * Converts the "email" attribute set by JwtHandshakeInterceptor into the
 * STOMP session's Principal, so convertAndSendToUser(email, ...) on the
 * server routes correctly to this session on the client.
 */
public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                       WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        if (email == null) {
            return super.determineUser(request, wsHandler, attributes);
        }
        return (Principal) () -> email;
    }
}