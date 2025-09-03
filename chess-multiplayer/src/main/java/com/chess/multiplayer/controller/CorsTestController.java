package com.chess.multiplayer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // This annotation also helps
public class CorsTestController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chess Backend is running!");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api")
    public ResponseEntity<Map<String, Object>> api() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chess Game API");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("websocket_endpoint", "/api/chess-websocket");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Backend is healthy");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("cors_enabled", "true");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/test-cors")
    public ResponseEntity<Map<String, String>> testCors() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "CORS test successful");
        response.put("origin", "All origins allowed");
        response.put("methods", "GET, POST, PUT, DELETE, OPTIONS");
        return ResponseEntity.ok(response);
    }
}