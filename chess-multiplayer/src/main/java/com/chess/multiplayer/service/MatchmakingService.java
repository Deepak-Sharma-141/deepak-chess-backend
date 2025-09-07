package com.chess.multiplayer.service;

import com.chess.multiplayer.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CompletableFuture;

@Service
@EnableScheduling
public class MatchmakingService {

    private final ConcurrentLinkedQueue<WaitingPlayer> waitingQueue = new ConcurrentLinkedQueue<>();
    private final Map<String, WaitingPlayer> sessionToPlayer = new ConcurrentHashMap<>();
    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    // Timeout in milliseconds (2 minutes)
    private static final long MATCH_TIMEOUT = 120000;

    @Autowired
    public MatchmakingService(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    public CompletableFuture<MatchResult> findRandomMatch(String sessionId, String playerName) {
        System.out.println("Player " + playerName + " (" + sessionId + ") requesting random match");

        // Remove player from queue if already waiting
        removePlayerFromQueue(sessionId);

        // Create waiting player
        WaitingPlayer currentPlayer = new WaitingPlayer(sessionId, playerName);
        sessionToPlayer.put(sessionId, currentPlayer);

        // Try to find an opponent immediately
        WaitingPlayer opponent = findAvailableOpponent(currentPlayer);

        if (opponent != null) {
            // Match found immediately
            System.out.println("Immediate match found: " + currentPlayer.getName() + " vs " + opponent.getName());
            return CompletableFuture.completedFuture(createMatch(currentPlayer, opponent));
        } else {
            // Add to waiting queue
            waitingQueue.offer(currentPlayer);
            System.out.println("Player " + currentPlayer.getName() + " added to waiting queue. Queue size: " + waitingQueue.size());

            // Send waiting message
            sendWaitingMessage(sessionId);

            // Return a future that will complete when match is found or timeout occurs
            return currentPlayer.getMatchFuture();
        }
    }

    private WaitingPlayer findAvailableOpponent(WaitingPlayer currentPlayer) {
        Iterator<WaitingPlayer> iterator = waitingQueue.iterator();
        while (iterator.hasNext()) {
            WaitingPlayer waitingPlayer = iterator.next();

            // Don't match with self and ensure opponent is still valid
            if (!waitingPlayer.getSessionId().equals(currentPlayer.getSessionId()) &&
                    !waitingPlayer.isExpired()) {

                iterator.remove();
                sessionToPlayer.remove(waitingPlayer.getSessionId());
                return waitingPlayer;
            }
        }
        return null;
    }

    private MatchResult createMatch(WaitingPlayer player1, WaitingPlayer player2) {
        try {
            // Randomly assign colors
            boolean player1IsWhite = Math.random() < 0.5;
            String player1Color = player1IsWhite ? "white" : "black";
            String player2Color = player1IsWhite ? "black" : "white";

            // Create game using existing GameService
            String gameId = gameService.createGame(player1.getSessionId(), player1.getName());
            GameSession game = gameService.joinGame(gameId, player2.getSessionId(), player2.getName());

            if (game != null) {
                System.out.println("Match created successfully: Game " + gameId +
                        " - " + player1.getName() + " (" + player1Color + ") vs " +
                        player2.getName() + " (" + player2Color + ")");

                // Send match found messages to both players
                sendMatchFoundMessage(player1, gameId, player1Color, player2.getName(), game);
                sendMatchFoundMessage(player2, gameId, player2Color, player1.getName(), game);

                // Complete both players' futures
                MatchResult result = new MatchResult(true, gameId, game);
                player1.getMatchFuture().complete(result);
                player2.getMatchFuture().complete(result);

                return result;
            } else {
                System.out.println("Failed to create game for matched players");
                return new MatchResult(false);
            }

        } catch (Exception e) {
            System.out.println("Error creating match: " + e.getMessage());
            e.printStackTrace();
            return new MatchResult(false);
        }
    }

    public void cancelMatchSearch(String sessionId) {
        System.out.println("Cancelling match search for session: " + sessionId);

        WaitingPlayer player = sessionToPlayer.remove(sessionId);
        if (player != null) {
            removePlayerFromQueue(sessionId);

            // Complete the future with cancellation result
            player.getMatchFuture().complete(new MatchResult(false, "cancelled"));

            // Send cancellation message
            sendMatchCancelledMessage(sessionId, "Match search cancelled by user");
        }
    }

    private void removePlayerFromQueue(String sessionId) {
        waitingQueue.removeIf(player -> player.getSessionId().equals(sessionId));
    }

    // Scheduled method to clean up expired players and handle timeouts
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void cleanupExpiredPlayers() {
        long currentTime = System.currentTimeMillis();

        Iterator<WaitingPlayer> iterator = waitingQueue.iterator();
        while (iterator.hasNext()) {
            WaitingPlayer player = iterator.next();

            if (player.isExpired()) {
                System.out.println("Player " + player.getName() + " match search timed out");

                iterator.remove();
                sessionToPlayer.remove(player.getSessionId());

                // Send timeout message
                sendMatchTimeoutMessage(player.getSessionId());

                // Complete future with timeout result
                player.getMatchFuture().complete(new MatchResult(false, "timeout"));
            }
        }

        // Log queue status
        if (!waitingQueue.isEmpty()) {
            System.out.println("Waiting queue status: " + waitingQueue.size() + " players waiting");
        }
    }

    // Handle player disconnection
    public void handlePlayerDisconnection(String sessionId) {
        System.out.println("Handling disconnection for session: " + sessionId);
        cancelMatchSearch(sessionId);
    }

    // WebSocket message sending methods
    private void sendWaitingMessage(String sessionId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "WAITING_FOR_OPPONENT");
        message.put("message", "Searching for an opponent...");
        message.put("queueSize", waitingQueue.size());

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
    }

    private void sendMatchFoundMessage(WaitingPlayer player, String gameId, String playerColor, String opponentName, GameSession game) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MATCH_FOUND");
        message.put("gameId", gameId);
        message.put("playerColor", playerColor);
        message.put("opponentName", opponentName);
        message.put("gameState", game);

        messagingTemplate.convertAndSendToUser(player.getSessionId(), "/queue/match", message);
    }

    private void sendMatchCancelledMessage(String sessionId, String reason) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MATCH_CANCELLED");
        message.put("reason", reason);

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
    }

    private void sendMatchTimeoutMessage(String sessionId) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "MATCH_TIMEOUT");
        message.put("reason", "No opponent found within the time limit. Please try again.");

        messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
    }

    // Get current queue status
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("waitingPlayers", waitingQueue.size());
        status.put("totalSessions", sessionToPlayer.size());

        List<Map<String, Object>> playersList = new ArrayList<>();
        for (WaitingPlayer player : waitingQueue) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("name", player.getName());
            playerInfo.put("waitTime", System.currentTimeMillis() - player.getJoinTime());
            playersList.add(playerInfo);
        }
        status.put("players", playersList);

        return status;
    }
}

// Supporting classes
