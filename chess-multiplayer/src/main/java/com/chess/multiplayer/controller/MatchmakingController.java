//package com.chess.multiplayer.controller;
//
//import com.chess.multiplayer.service.MatchmakingService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//
//import java.security.Principal;
//import java.util.Map;
//
//@Controller
//public class MatchmakingController {
//
//    @Autowired
//    private MatchmakingService matchmakingService;
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    @MessageMapping("/findRandomMatch")
//    public void findRandomMatch(@Payload Map<String, String> request, Principal principal) {
//        try {
//            String sessionId = principal.getName();
//            String playerName = request.get("playerName");
//            String type = request.get("type");
//
//            System.out.println("=== MATCHMAKING DEBUG ===");
//            System.out.println("Session ID: " + sessionId);
//            System.out.println("Player Name: " + playerName);
//            System.out.println("Request Type: " + type);
//            System.out.println("Current Queue Size: " + matchmakingService.getQueueStatus().get("waitingPlayers"));
//            System.out.println("========================");
//
//
//            System.out.println("Random match request from " + playerName + " (session: " + sessionId + ")");
//
//            if (playerName == null || playerName.trim().isEmpty()) {
//                sendErrorMessage(sessionId, "Player name is required");
//                return;
//            }
//
//            if (!"RANDOM_MATCH".equals(type)) {
//                sendErrorMessage(sessionId, "Invalid request type");
//                return;
//            }
//
//            // Start matchmaking process (this is async)
//            System.out.println("Starting matchmaking for: " + playerName);
//            matchmakingService.findRandomMatch(sessionId, playerName.trim())
//                    .thenAccept(result -> {
//                        // This will be called when match is found, cancelled, or timeout occurs
//                        if (!result.isSuccess()) {
//                            String reason = result.getReason();
//                            if ("timeout".equals(reason)) {
//                                sendTimeoutMessage(sessionId);
//                            } else if ("cancelled".equals(reason)) {
//                                sendCancelledMessage(sessionId, "Match search was cancelled");
//                            } else {
//                                sendErrorMessage(sessionId, "Failed to find match");
//                            }
//                        }
//                        // Success case is handled in MatchmakingService.createMatch()
//                    })
//                    .exceptionally(throwable -> {
//                        System.out.println("Error in matchmaking: " + throwable.getMessage());
//                        sendErrorMessage(sessionId, "An error occurred while searching for match");
//                        return null;
//                    });
//
//        } catch (Exception e) {
//            System.out.println("Exception in findRandomMatch: " + e.getMessage());
//            e.printStackTrace();
//            sendErrorMessage(principal.getName(), "Server error occurred");
//        }
//    }
//
//    @GetMapping("/api/matchmaking/status")
//    @ResponseBody
//    public Map<String, Object> getMatchmakingStatus() {
//        return matchmakingService.getQueueStatus();
//    }
//
//    @MessageMapping("/cancelRandomMatch")
//    public void cancelRandomMatch(@Payload Map<String, String> request, Principal principal) {
//        try {
//            String sessionId = principal.getName();
//            String playerName = request.get("playerName");
//
//            System.out.println("Cancel match request from " + playerName + " (session: " + sessionId + ")");
//
//            matchmakingService.cancelMatchSearch(sessionId);
//
//        } catch (Exception e) {
//            System.out.println("Exception in cancelRandomMatch: " + e.getMessage());
//            sendErrorMessage(principal.getName(), "Error cancelling match search");
//        }
//    }
//
//    // WebSocket event handlers for connection/disconnection
//    @MessageMapping("/connect")
//    public void handleConnect(Principal principal) {
//        System.out.println("Player connected: " + principal.getName());
//    }
//
//    @MessageMapping("/disconnect")
//    public void handleDisconnect(@Payload Map<String, String> request, Principal principal) {
//        String sessionId = principal.getName();
//        System.out.println("Player disconnected: " + sessionId);
//
//        matchmakingService.handlePlayerDisconnection(sessionId);
//    }
//
//    // Helper methods to send different types of messages
//    private void sendErrorMessage(String sessionId, String errorMessage) {
//        Map<String, Object> message = Map.of(
//                "type", "MATCH_ERROR",
//                "message", errorMessage
//        );
//        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//    }
//
//    private void sendTimeoutMessage(String sessionId) {
//        Map<String, Object> message = Map.of(
//                "type", "MATCH_TIMEOUT",
//                "message", "No opponent found within the time limit. Please try again.",
//                "timeout", true
//        );
//        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//    }
//
//    private void sendCancelledMessage(String sessionId, String reason) {
//        Map<String, Object> message = Map.of(
//                "type", "MATCH_CANCELLED",
//                "message", reason,
//                "cancelled", true
//        );
//        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//    }
//}
//

//package com.chess.multiplayer.controller;
//
//import com.chess.multiplayer.service.MatchmakingService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.http.ResponseEntity;
//
//import java.security.Principal;
//import java.util.Map;
//
//@Controller
//public class MatchmakingController {
//
//    @Autowired
//    private MatchmakingService matchmakingService;
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    @MessageMapping("/findRandomMatch")
//    public void findRandomMatch(@Payload Map<String, String> request, Principal principal) {
//        try {
//            String sessionId = principal.getName();
//            String playerName = request.get("playerName");
//            String type = request.get("type");
//
//            System.out.println("=== MATCHMAKING DEBUG ===");
//            System.out.println("Session ID: " + sessionId);
//            System.out.println("Player Name: " + playerName);
//            System.out.println("Request Type: " + type);
//            System.out.println("Principal: " + principal);
//            System.out.println("Current Queue Size: " + matchmakingService.getQueueStatus().get("waitingPlayers"));
//            System.out.println("========================");
//
//            if (playerName == null || playerName.trim().isEmpty()) {
//                sendErrorMessage(sessionId, "Player name is required");
//                return;
//            }
//
//            if (!"RANDOM_MATCH".equals(type)) {
//                sendErrorMessage(sessionId, "Invalid request type");
//                return;
//            }
//
//            // Start matchmaking process (this is async)
//            System.out.println("Starting matchmaking for: " + playerName);
//            matchmakingService.findRandomMatch(sessionId, playerName.trim())
//                    .thenAccept(result -> {
//                        // This will be called when match is found, cancelled, or timeout occurs
//                        if (!result.isSuccess()) {
//                            String reason = result.getReason();
//                            System.out.println("Match failed for " + sessionId + ": " + reason);
//                            if ("timeout".equals(reason)) {
//                                sendTimeoutMessage(sessionId);
//                            } else if ("cancelled".equals(reason)) {
//                                sendCancelledMessage(sessionId, "Match search was cancelled");
//                            } else {
//                                sendErrorMessage(sessionId, "Failed to find match: " + reason);
//                            }
//                        } else {
//                            System.out.println("Match successful for " + sessionId);
//                        }
//                        // Success case is handled in MatchmakingService.createMatch()
//                    })
//                    .exceptionally(throwable -> {
//                        System.err.println("Error in matchmaking for " + sessionId + ": " + throwable.getMessage());
//                        throwable.printStackTrace();
//                        sendErrorMessage(sessionId, "An error occurred while searching for match");
//                        return null;
//                    });
//
//        } catch (Exception e) {
//            System.err.println("Exception in findRandomMatch: " + e.getMessage());
//            e.printStackTrace();
//            sendErrorMessage(principal.getName(), "Server error occurred");
//        }
//    }
//
//    @MessageMapping("/cancelRandomMatch")
//    public void cancelRandomMatch(@Payload Map<String, String> request, Principal principal) {
//        try {
//            String sessionId = principal.getName();
//            String playerName = request.get("playerName");
//
//            System.out.println("Cancel match request from " + playerName + " (session: " + sessionId + ")");
//
//            matchmakingService.cancelMatchSearch(sessionId);
//
//        } catch (Exception e) {
//            System.err.println("Exception in cancelRandomMatch: " + e.getMessage());
//            sendErrorMessage(principal.getName(), "Error cancelling match search");
//        }
//    }
//
//    // WebSocket event handlers for connection/disconnection
//    @MessageMapping("/connect")
//    public void handleConnect(Principal principal) {
//        System.out.println("Player connected: " + principal.getName());
//    }
//
//    @MessageMapping("/disconnect")
//    public void handleDisconnect(@Payload Map<String, String> request, Principal principal) {
//        String sessionId = principal.getName();
//        System.out.println("Player disconnected: " + sessionId);
//
//        matchmakingService.handlePlayerDisconnection(sessionId);
//    }
//
//    // REST endpoint for status - moved here to avoid conflicts
//    @GetMapping("/api/matchmaking/status")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> getMatchmakingStatus() {
//        try {
//            Map<String, Object> status = matchmakingService.getQueueStatus();
//            return ResponseEntity.ok(status);
//        } catch (Exception e) {
//            System.err.println("Error getting matchmaking status: " + e.getMessage());
//            return ResponseEntity.internalServerError()
//                    .body(Map.of("error", "Failed to get status", "message", e.getMessage()));
//        }
//    }
//
//    @PostMapping("/api/matchmaking/cancel/{sessionId}")
//    @ResponseBody
//    public ResponseEntity<Map<String, Object>> cancelMatchmakingRest(@PathVariable String sessionId) {
//        try {
//            matchmakingService.cancelMatchSearch(sessionId);
//            return ResponseEntity.ok(Map.of("success", true, "message", "Match search cancelled"));
//        } catch (Exception e) {
//            System.err.println("Error cancelling match for " + sessionId + ": " + e.getMessage());
//            return ResponseEntity.internalServerError()
//                    .body(Map.of("success", false, "message", "Failed to cancel match"));
//        }
//    }
//
//    // Helper methods to send different types of messages
//    private void sendErrorMessage(String sessionId, String errorMessage) {
//        try {
//            Map<String, Object> message = Map.of(
//                    "type", "MATCH_ERROR",
//                    "message", errorMessage
//            );
//            System.out.println("Sending error message to " + sessionId + ": " + errorMessage);
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.err.println("Failed to send error message to " + sessionId + ": " + e.getMessage());
//        }
//    }
//
//    private void sendTimeoutMessage(String sessionId) {
//        try {
//            Map<String, Object> message = Map.of(
//                    "type", "MATCH_TIMEOUT",
//                    "message", "No opponent found within the time limit. Please try again.",
//                    "timeout", true
//            );
//            System.out.println("Sending timeout message to " + sessionId);
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.err.println("Failed to send timeout message to " + sessionId + ": " + e.getMessage());
//        }
//    }
//
//    private void sendCancelledMessage(String sessionId, String reason) {
//        try {
//            Map<String, Object> message = Map.of(
//                    "type", "MATCH_CANCELLED",
//                    "message", reason,
//                    "cancelled", true
//            );
//            System.out.println("Sending cancelled message to " + sessionId + ": " + reason);
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.err.println("Failed to send cancelled message to " + sessionId + ": " + e.getMessage());
//        }
//    }
//}

package com.chess.multiplayer.controller;

import com.chess.multiplayer.service.MatchmakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@Controller
public class MatchmakingController {

    @Autowired
    private MatchmakingService matchmakingService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/findRandomMatch")
    public void findRandomMatch(@Payload Map<String, String> request, Principal principal,
                                SimpMessageHeaderAccessor headerAccessor) {
        try {
            // Handle null principal by using session ID from multiple sources
            String sessionId = extractSessionId(principal, headerAccessor);
            String playerName = request.get("playerName");
            String type = request.get("type");

            System.out.println("=== MATCHMAKING DEBUG ===");
            System.out.println("Session ID: " + sessionId);
            System.out.println("Player Name: " + playerName);
            System.out.println("Request Type: " + type);
            System.out.println("Principal: " + principal);
            System.out.println("Header Session: " + (headerAccessor != null ? headerAccessor.getSessionId() : "null"));
            System.out.println("Current Queue Size: " + matchmakingService.getQueueStatus().get("waitingPlayers"));
            System.out.println("========================");

            if (playerName == null || playerName.trim().isEmpty()) {
                sendErrorMessage(sessionId, "Player name is required");
                return;
            }

            if (!"RANDOM_MATCH".equals(type)) {
                sendErrorMessage(sessionId, "Invalid request type");
                return;
            }

            // Start matchmaking process
            System.out.println("Starting matchmaking for: " + playerName + " (session: " + sessionId + ")");
            matchmakingService.findRandomMatch(sessionId, playerName.trim())
                    .thenAccept(result -> {
                        if (!result.isSuccess()) {
                            String reason = result.getReason();
                            System.out.println("Match failed for " + sessionId + ": " + reason);
                            if ("timeout".equals(reason)) {
                                sendTimeoutMessage(sessionId);
                            } else if ("cancelled".equals(reason)) {
                                sendCancelledMessage(sessionId, "Match search was cancelled");
                            } else {
                                sendErrorMessage(sessionId, "Failed to find match: " + reason);
                            }
                        } else {
                            System.out.println("Match successful for " + sessionId);
                        }
                    })
                    .exceptionally(throwable -> {
                        System.err.println("Error in matchmaking for " + sessionId + ": " + throwable.getMessage());
                        throwable.printStackTrace();
                        sendErrorMessage(sessionId, "An error occurred while searching for match");
                        return null;
                    });

        } catch (Exception e) {
            System.err.println("Exception in findRandomMatch: " + e.getMessage());
            e.printStackTrace();
            String fallbackSessionId = extractSessionId(principal, headerAccessor);
            sendErrorMessage(fallbackSessionId, "Server error occurred");
        }
    }

    @MessageMapping("/cancelRandomMatch")
    public void cancelRandomMatch(@Payload Map<String, String> request, Principal principal,
                                  SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = extractSessionId(principal, headerAccessor);
            String playerName = request.get("playerName");

            System.out.println("Cancel match request from " + playerName + " (session: " + sessionId + ")");
            matchmakingService.cancelMatchSearch(sessionId);

        } catch (Exception e) {
            System.err.println("Exception in cancelRandomMatch: " + e.getMessage());
            String fallbackSessionId = extractSessionId(principal, headerAccessor);
            sendErrorMessage(fallbackSessionId, "Error cancelling match search");
        }
    }

    @MessageMapping("/testConnection")
    public void testConnection(@Payload Map<String, String> request, Principal principal,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = extractSessionId(principal, headerAccessor);

            System.out.println("=== CONNECTION TEST ===");
            System.out.println("Session ID: " + sessionId);
            System.out.println("Principal: " + principal);
            System.out.println("Header Session: " + (headerAccessor != null ? headerAccessor.getSessionId() : "null"));
            System.out.println("Request: " + request);
            System.out.println("=====================");

            // Send back a test response
            Map<String, Object> response = Map.of(
                    "type", "CONNECTION_TEST_RESPONSE",
                    "sessionId", sessionId,
                    "message", "Connection test successful!",
                    "timestamp", System.currentTimeMillis()
            );

            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", response);

        } catch (Exception e) {
            System.err.println("Connection test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to extract session ID from multiple sources
    private String extractSessionId(Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal != null && principal.getName() != null) {
            return principal.getName();
        }

        if (headerAccessor != null && headerAccessor.getSessionId() != null) {
            System.out.println("FALLBACK: Using session ID from header: " + headerAccessor.getSessionId());
            return headerAccessor.getSessionId();
        }

        // Last resort: generate a temporary session ID
        String tempSessionId = "temp_" + UUID.randomUUID().toString().substring(0, 8);
        System.out.println("GENERATED: Temporary session ID: " + tempSessionId);
        return tempSessionId;
    }

    // REST endpoints for status and debugging
    @GetMapping("/api/matchmaking/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMatchmakingStatus() {
        try {
            Map<String, Object> status = matchmakingService.getQueueStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            System.err.println("Error getting matchmaking status: " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get status", "message", e.getMessage()));
        }
    }

    @PostMapping("/api/matchmaking/cancel/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cancelMatchmakingRest(@PathVariable String sessionId) {
        try {
            matchmakingService.cancelMatchSearch(sessionId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Match search cancelled"));
        } catch (Exception e) {
            System.err.println("Error cancelling match for " + sessionId + ": " + e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "Failed to cancel match"));
        }
    }

    // Message sending helper methods
    private void sendErrorMessage(String sessionId, String errorMessage) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "MATCH_ERROR",
                    "message", errorMessage
            );
            System.out.println("Sending error message to " + sessionId + ": " + errorMessage);
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
        } catch (Exception e) {
            System.err.println("Failed to send error message to " + sessionId + ": " + e.getMessage());
        }
    }

    private void sendTimeoutMessage(String sessionId) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "MATCH_TIMEOUT",
                    "message", "No opponent found within the time limit. Please try again.",
                    "timeout", true
            );
            System.out.println("Sending timeout message to " + sessionId);
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
        } catch (Exception e) {
            System.err.println("Failed to send timeout message to " + sessionId + ": " + e.getMessage());
        }
    }

    private void sendCancelledMessage(String sessionId, String reason) {
        try {
            Map<String, Object> message = Map.of(
                    "type", "MATCH_CANCELLED",
                    "message", reason,
                    "cancelled", true
            );
            System.out.println("Sending cancelled message to " + sessionId + ": " + reason);
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
        } catch (Exception e) {
            System.err.println("Failed to send cancelled message to " + sessionId + ": " + e.getMessage());
        }
    }


}
