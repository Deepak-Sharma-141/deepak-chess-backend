package com.chess.multiplayer.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // Allow all origins
public class SimpleApiController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Chess Backend is running!");
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("endpoints", new String[]{"/api", "/api/health", "/api/test"});
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
        response.put("cors_enabled", true);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Backend test successful - " + LocalDateTime.now());
    }
}