//package com.chess.multiplayer.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.messaging.simp.config.MessageBrokerRegistry;
//import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
//import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
//import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
//
//@Configuration
//@EnableWebSocketMessageBroker
//public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
//
//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        config.enableSimpleBroker("/topic", "/queue");
//        config.setApplicationDestinationPrefixes("/app");
//    }
//
//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/chess-websocket")
//                .setAllowedOriginPatterns("*")  // Allow all origins for now
//                .withSockJS()
//                .setStreamBytesLimit(512 * 1024)
//                .setHttpMessageCacheSize(1000)
//                .setDisconnectDelay(30 * 1000);
//    }
//}

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
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chess-websocket")
                .setAllowedOriginPatterns(
                        "https://deepak-sharma-141.github.io",     // Your GitHub Pages
                        "https://*.github.io",                      // Other GitHub Pages
                        "https://*.up.railway.app",                 // Railway domains
                        "http://localhost:3000",                    // Local development
                        "http://localhost:3001",                    // Alternative local port
                        "https://localhost:3000",                   // Local HTTPS
                        "http://127.0.0.1:3000",                   // Local IP
                        "https://127.0.0.1:3000"                   // Local HTTPS IP
                )
                .withSockJS()
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000);
    }
}