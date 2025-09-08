package com.chess.multiplayer.controller;

import com.chess.multiplayer.service.MatchmakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
public class MatchmakingController {

    @Autowired
    private MatchmakingService matchmakingService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/findRandomMatch")
    public void findRandomMatch(@Payload Map<String, String> request, Principal principal) {
        try {
            String sessionId = principal.getName();
            String playerName = request.get("playerName");
            String type = request.get("type");

            System.out.println("=== MATCHMAKING DEBUG ===");
            System.out.println("Session ID: " + sessionId);
            System.out.println("Player Name: " + playerName);
            System.out.println("Request Type: " + type);
            System.out.println("Current Queue Size: " + matchmakingService.getQueueStatus().get("waitingPlayers"));
            System.out.println("========================");


            System.out.println("Random match request from " + playerName + " (session: " + sessionId + ")");

            if (playerName == null || playerName.trim().isEmpty()) {
                sendErrorMessage(sessionId, "Player name is required");
                return;
            }

            if (!"RANDOM_MATCH".equals(type)) {
                sendErrorMessage(sessionId, "Invalid request type");
                return;
            }

            // Start matchmaking process (this is async)
            System.out.println("Starting matchmaking for: " + playerName);
            matchmakingService.findRandomMatch(sessionId, playerName.trim())
                    .thenAccept(result -> {
                        // This will be called when match is found, cancelled, or timeout occurs
                        if (!result.isSuccess()) {
                            String reason = result.getReason();
                            if ("timeout".equals(reason)) {
                                sendTimeoutMessage(sessionId);
                            } else if ("cancelled".equals(reason)) {
                                sendCancelledMessage(sessionId, "Match search was cancelled");
                            } else {
                                sendErrorMessage(sessionId, "Failed to find match");
                            }
                        }
                        // Success case is handled in MatchmakingService.createMatch()
                    })
                    .exceptionally(throwable -> {
                        System.out.println("Error in matchmaking: " + throwable.getMessage());
                        sendErrorMessage(sessionId, "An error occurred while searching for match");
                        return null;
                    });

        } catch (Exception e) {
            System.out.println("Exception in findRandomMatch: " + e.getMessage());
            e.printStackTrace();
            sendErrorMessage(principal.getName(), "Server error occurred");
        }
    }

    @GetMapping("/api/matchmaking/status")
    @ResponseBody
    public Map<String, Object> getMatchmakingStatus() {
        return matchmakingService.getQueueStatus();
    }

    @MessageMapping("/cancelRandomMatch")
    public void cancelRandomMatch(@Payload Map<String, String> request, Principal principal) {
        try {
            String sessionId = principal.getName();
            String playerName = request.get("playerName");

            System.out.println("Cancel match request from " + playerName + " (session: " + sessionId + ")");

            matchmakingService.cancelMatchSearch(sessionId);

        } catch (Exception e) {
            System.out.println("Exception in cancelRandomMatch: " + e.getMessage());
            sendErrorMessage(principal.getName(), "Error cancelling match search");
        }
    }

    // WebSocket event handlers for connection/disconnection
    @MessageMapping("/connect")
    public void handleConnect(Principal principal) {
        System.out.println("Player connected: " + principal.getName());
    }

    @MessageMapping("/disconnect")
    public void handleDisconnect(@Payload Map<String, String> request, Principal principal) {
        String sessionId = principal.getName();
        System.out.println("Player disconnected: " + sessionId);

        matchmakingService.handlePlayerDisconnection(sessionId);
    }

    // Helper methods to send different types of messages
    private void sendErrorMessage(String sessionId, String errorMessage) {
        Map<String, Object> message = Map.of(
                "type", "MATCH_ERROR",
                "message", errorMessage
        );
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
    }

    private void sendTimeoutMessage(String sessionId) {
        Map<String, Object> message = Map.of(
                "type", "MATCH_TIMEOUT",
                "message", "No opponent found within the time limit. Please try again.",
                "timeout", true
        );
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
    }

    private void sendCancelledMessage(String sessionId, String reason) {
        Map<String, Object> message = Map.of(
                "type", "MATCH_CANCELLED",
                "message", reason,
                "cancelled", true
        );
        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
    }
}

