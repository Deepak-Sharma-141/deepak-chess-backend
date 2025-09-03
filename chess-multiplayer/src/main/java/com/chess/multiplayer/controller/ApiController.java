package com.chess.multiplayer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"https://deepak-sharma-141.github.io", "http://localhost:*"})
public class ApiController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> apiRoot() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chess Game API");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("version", "1.0.0");
        response.put("available_endpoints", Map.of(
                "health", "/api/health",
                "websocket", "/api/chess-websocket",
                "websocket_info", "/api/chess-websocket/info",
                "test_cors", "/api/test-cors"
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Chess backend is healthy");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("websocket_endpoint", "/api/chess-websocket");
        response.put("server_info", Map.of(
                "java_version", System.getProperty("java.version"),
                "spring_boot", "3.2.0"
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/test-cors")
    public ResponseEntity<Map<String, String>> testCors() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "CORS is working correctly");
        response.put("origin_allowed", "https://deepak-sharma-141.github.io");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}