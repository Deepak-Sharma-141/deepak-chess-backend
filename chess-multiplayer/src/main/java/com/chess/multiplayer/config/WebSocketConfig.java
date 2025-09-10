package com.chess.multiplayer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.UUID;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue", "/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chess-websocket")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);

    }

        // CRITICAL FIX: Add handshake interceptor to set session principal
        @Override
        public void configureClientInboundChannel(ChannelRegistration registration) {
            registration.interceptors(new ChannelInterceptor() {
                @Override
                public Message<?> preSend(Message<?> message, MessageChannel channel) {
                    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                    if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                        // Generate or extract session ID
                        String sessionId = accessor.getSessionId();
                        if (sessionId == null || sessionId.isEmpty()) {
                            sessionId = "session_" + UUID.randomUUID().toString().substring(0, 8);
                        }

                        System.out.println("=== WebSocket CONNECT ===");
                        System.out.println("Generated Session ID: " + sessionId);
                        System.out.println("Headers: " + accessor.toNativeHeaderMap());
                        System.out.println("========================");

                        // Set the principal with the session ID
                        final String finalSessionId = sessionId;

                        // Set the principal with the session ID
                        accessor.setUser(new Principal() {
                            @Override
                            public String getName() {
                                return finalSessionId;
                            }
                        });

                        System.out.println("WebSocket CONNECT - Session ID: " + sessionId);
                    }

                    return message;
                }
            });

        }
}