package com.chess.multiplayer.config;

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
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");  // Add user-specific destinations
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chess-websocket")
                .setAllowedOrigins(
                        "https://deepak-sharma-141.github.io/deepak-chess-frontend/",  // Replace with your GitHub Pages URL
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://127.0.0.1:3000",
                        "http://127.0.0.1:8080"
                )
                .withSockJS()
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);

        // Also add endpoint without SockJS for native WebSocket support
        registry.addEndpoint("/chess-websocket")
                .setAllowedOrigins(
                        "https://deepak-sharma-141.github.io/deepak-chess-frontend/",  // Replace with your GitHub Pages URL
                        "http://localhost:3000",
                        "http://localhost:8080",
                        "http://127.0.0.1:3000",
                        "http://127.0.0.1:8080"
                );
    }
}