//package com.chess.multiplayer.service;
//
//import com.chess.multiplayer.model.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//@EnableScheduling
//public class MatchmakingService {
//
//    private final ConcurrentLinkedQueue<WaitingPlayer> waitingQueue = new ConcurrentLinkedQueue<>();
//    private final Map<String, WaitingPlayer> sessionToPlayer = new ConcurrentHashMap<>();
//    private final Object queueLock = new Object();
//    // Change from constructor injection to field injection to break circular dependency
//    @Autowired
//    private GameService gameService;
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    // Timeout in milliseconds (2 minutes)
//    private static final long MATCH_TIMEOUT = 120000;
//
//    // Remove constructor - Spring will use default constructor and field injection
//
//    @Async
//    public CompletableFuture<MatchResult> findRandomMatch(String sessionId, String playerName) {
//        System.out.println("Player " + playerName + " (" + sessionId + ") requesting random match");
//        synchronized (queueLock) {
//            // Remove player from queue if already waiting
//            removePlayerFromQueue(sessionId);
//
//            // Create waiting player
//            WaitingPlayer currentPlayer = new WaitingPlayer(sessionId, playerName);
//            sessionToPlayer.put(sessionId, currentPlayer);
//
//            // Try to find an opponent immediately
//            WaitingPlayer opponent = findAvailableOpponent(currentPlayer);
//
//            if (opponent != null) {
//                // Match found immediately
//                System.out.println("Immediate match found: " + currentPlayer.getName() + " vs " + opponent.getName());
//                return CompletableFuture.completedFuture(createMatch(currentPlayer, opponent));
//            } else {
//                // Add to waiting queue
//                waitingQueue.offer(currentPlayer);
//                System.out.println("Player " + currentPlayer.getName() + " added to waiting queue. Queue size: " + waitingQueue.size());
//
//                // Send waiting message
//                sendWaitingMessage(sessionId);
//
//                // Return a future that will complete when match is found or timeout occurs
//                return currentPlayer.getMatchFuture();
//            }
//        }
//    }
//
//    private WaitingPlayer findAvailableOpponent(WaitingPlayer currentPlayer) {
//        Iterator<WaitingPlayer> iterator = waitingQueue.iterator();
//        while (iterator.hasNext()) {
//            WaitingPlayer waitingPlayer = iterator.next();
//
//            // Don't match with self and ensure opponent is still valid
//            if (!waitingPlayer.getSessionId().equals(currentPlayer.getSessionId()) &&
//                    !waitingPlayer.isExpired()) {
//
//                iterator.remove();
//                sessionToPlayer.remove(waitingPlayer.getSessionId());
//                return waitingPlayer;
//            }
//        }
//        return null;
//    }
//
//    private MatchResult createMatch(WaitingPlayer player1, WaitingPlayer player2) {
//        try {
//            // Randomly assign colors
//            boolean player1IsWhite = Math.random() < 0.5;
//            String player1Color = player1IsWhite ? "white" : "black";
//            String player2Color = player1IsWhite ? "black" : "white";
//
//            // Create game using existing GameService
//            String gameId = gameService.createGame(player1.getSessionId(), player1.getName());
//            GameSession game = gameService.joinGame(gameId, player2.getSessionId(), player2.getName());
//
//            if (game != null) {
//                System.out.println("Match created successfully: Game " + gameId +
//                        " - " + player1.getName() + " (" + player1Color + ") vs " +
//                        player2.getName() + " (" + player2Color + ")");
//
//                // Send match found messages to both players
//                sendMatchFoundMessage(player1, gameId, player1Color, player2.getName(), game);
//                sendMatchFoundMessage(player2, gameId, player2Color, player1.getName(), game);
//
//                // Complete both players' futures
//                MatchResult result = new MatchResult(true, gameId, game);
//                player1.getMatchFuture().complete(result);
//                player2.getMatchFuture().complete(result);
//
//                return result;
//            } else {
//                System.out.println("Failed to create game for matched players");
//                return new MatchResult(false);
//            }
//
//        } catch (Exception e) {
//            System.out.println("Error creating match: " + e.getMessage());
//            e.printStackTrace();
//            return new MatchResult(false);
//        }
//    }
//
//    public void cancelMatchSearch(String sessionId) {
//        System.out.println("Cancelling match search for session: " + sessionId);
//
//        WaitingPlayer player = sessionToPlayer.remove(sessionId);
//        if (player != null) {
//            removePlayerFromQueue(sessionId);
//
//            // Complete the future with cancellation result
//            player.getMatchFuture().complete(new MatchResult(false, "cancelled"));
//
//            // Send cancellation message
//            sendMatchCancelledMessage(sessionId, "Match search cancelled by user");
//        }
//    }
//
//    private void removePlayerFromQueue(String sessionId) {
//        waitingQueue.removeIf(player -> player.getSessionId().equals(sessionId));
//    }
//
//    // Scheduled method to clean up expired players and handle timeouts
//    @Scheduled(fixedRate = 30000) // Run every 30 seconds
//    public void cleanupExpiredPlayers() {
//        long currentTime = System.currentTimeMillis();
//
//        Iterator<WaitingPlayer> iterator = waitingQueue.iterator();
//        while (iterator.hasNext()) {
//            WaitingPlayer player = iterator.next();
//
//            if (player.isExpired()) {
//                System.out.println("Player " + player.getName() + " match search timed out");
//
//                iterator.remove();
//                sessionToPlayer.remove(player.getSessionId());
//
//                // Send timeout message
//                sendMatchTimeoutMessage(player.getSessionId());
//
//                // Complete future with timeout result
//                player.getMatchFuture().complete(new MatchResult(false, "timeout"));
//            }
//        }
//
//        // Log queue status
//        if (!waitingQueue.isEmpty()) {
//            System.out.println("Waiting queue status: " + waitingQueue.size() + " players waiting");
//        }
//    }
//
//    // Handle player disconnection
//    public void handlePlayerDisconnection(String sessionId) {
//        System.out.println("Handling disconnection for session: " + sessionId);
//        cancelMatchSearch(sessionId);
//    }
//
//    // WebSocket message sending methods
//    private void sendWaitingMessage(String sessionId) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "WAITING_FOR_OPPONENT");
//            message.put("message", "Searching for an opponent...");
//            message.put("queueSize", waitingQueue.size());
//
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending waiting message: " + e.getMessage());
//        }
//    }
//
//    private void sendMatchFoundMessage(WaitingPlayer player, String gameId, String playerColor, String opponentName, GameSession game) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "MATCH_FOUND");
//            message.put("gameId", gameId);
//            message.put("playerColor", playerColor);
//            message.put("opponentName", opponentName);
//            message.put("gameState", game);
//
//            messagingTemplate.convertAndSendToUser(player.getSessionId(), "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending match found message: " + e.getMessage());
//        }
//    }
//
//    private void sendMatchCancelledMessage(String sessionId, String reason) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "MATCH_CANCELLED");
//            message.put("reason", reason);
//
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending match cancelled message: " + e.getMessage());
//        }
//    }
//
//    private void sendMatchTimeoutMessage(String sessionId) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "MATCH_TIMEOUT");
//            message.put("reason", "No opponent found within the time limit. Please try again.");
//
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending match timeout message: " + e.getMessage());
//        }
//    }
//
//    // Get current queue status
//    public Map<String, Object> getQueueStatus() {
//        Map<String, Object> status = new HashMap<>();
//        status.put("waitingPlayers", waitingQueue.size());
//        status.put("totalSessions", sessionToPlayer.size());
//
//        List<Map<String, Object>> playersList = new ArrayList<>();
//        for (WaitingPlayer player : waitingQueue) {
//            Map<String, Object> playerInfo = new HashMap<>();
//            playerInfo.put("name", player.getName());
//            playerInfo.put("waitTime", System.currentTimeMillis() - player.getJoinTime());
//            playersList.add(playerInfo);
//        }
//        status.put("players", playersList);
//
//        return status;
//    }
//}

//package com.chess.multiplayer.service;
//
//import com.chess.multiplayer.model.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.messaging.simp.SimpMessagingTemplate;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.ConcurrentHashMap;
//import java.util.concurrent.ConcurrentLinkedQueue;
//import java.util.concurrent.CompletableFuture;
//
//@Service
//@EnableScheduling
//public class MatchmakingService {
//
//    private final ConcurrentLinkedQueue<WaitingPlayer> waitingQueue = new ConcurrentLinkedQueue<>();
//    private final Map<String, WaitingPlayer> sessionToPlayer = new ConcurrentHashMap<>();
//    private final Object queueLock = new Object();
//
//    @Autowired
//    private GameService gameService;
//
//    @Autowired
//    private SimpMessagingTemplate messagingTemplate;
//
//    // Timeout in milliseconds (2 minutes)
//    private static final long MATCH_TIMEOUT = 120000;
//
//    @Async
//    public CompletableFuture<MatchResult> findRandomMatch(String sessionId, String playerName) {
//        System.out.println("Player " + playerName + " (" + sessionId + ") requesting random match");
//
//        synchronized (queueLock) {
//            // Remove player from queue if already waiting (prevent duplicates)
//            removePlayerFromQueue(sessionId);
//            sessionToPlayer.remove(sessionId);
//
//            // Create waiting player
//            WaitingPlayer currentPlayer = new WaitingPlayer(sessionId, playerName);
//            sessionToPlayer.put(sessionId, currentPlayer);
//
//            // Try to find an opponent immediately
//            WaitingPlayer opponent = findAvailableOpponent(currentPlayer);
//
//            if (opponent != null) {
//                // Match found immediately
//                System.out.println("Immediate match found: " + currentPlayer.getName() + " vs " + opponent.getName());
//
//                // Remove opponent from tracking maps
//                sessionToPlayer.remove(opponent.getSessionId());
//                sessionToPlayer.remove(currentPlayer.getSessionId());
//
//                MatchResult result = createMatch(currentPlayer, opponent);
//                return CompletableFuture.completedFuture(result);
//            } else {
//                // Add to waiting queue
//                waitingQueue.offer(currentPlayer);
//                System.out.println("Player " + currentPlayer.getName() + " added to waiting queue. Queue size: " + waitingQueue.size());
//
//                // Send waiting message
//                sendWaitingMessage(sessionId, waitingQueue.size());
//
//                // Return the future that will complete when match is found or timeout occurs
//                return currentPlayer.getMatchFuture();
//            }
//        }
//    }
//
//    private WaitingPlayer findAvailableOpponent(WaitingPlayer currentPlayer) {
//        Iterator<WaitingPlayer> iterator = waitingQueue.iterator();
//        while (iterator.hasNext()) {
//            WaitingPlayer waitingPlayer = iterator.next();
//
//            // Don't match with self and ensure opponent is still valid
//            if (!waitingPlayer.getSessionId().equals(currentPlayer.getSessionId()) &&
//                    !waitingPlayer.isExpired() &&
//                    sessionToPlayer.containsKey(waitingPlayer.getSessionId())) {
//
//                // Remove from queue
//                iterator.remove();
//                System.out.println("Found opponent: " + waitingPlayer.getName() + " for " + currentPlayer.getName());
//                return waitingPlayer;
//            } else if (waitingPlayer.isExpired()) {
//                // Clean up expired player
//                iterator.remove();
//                sessionToPlayer.remove(waitingPlayer.getSessionId());
//                System.out.println("Removed expired player: " + waitingPlayer.getName());
//            }
//        }
//        return null;
//    }
//
//    private MatchResult createMatch(WaitingPlayer player1, WaitingPlayer player2) {
//        try {
//            System.out.println("Creating match between " + player1.getName() + " and " + player2.getName());
//
//            // Randomly assign colors
//            boolean player1IsWhite = Math.random() < 0.5;
//            String player1Color = player1IsWhite ? "white" : "black";
//            String player2Color = player1IsWhite ? "black" : "white";
//
//            // Create game using existing GameService
//            String gameId = gameService.createGame(player1.getSessionId(), player1.getName());
//            //GameSession game = gameService.joinGame(gameId, player2.getSessionId(), player2.getName());
//
//            GameSession game = null;
//            if (gameId != null) {
//                game = gameService.joinGame(gameId, player2.getSessionId(), player2.getName());
//            }
//
//            if (game != null && gameId != null) {
//                System.out.println("Match created successfully: Game " + gameId +
//                        " - " + player1.getName() + " (" + player1Color + ") vs " +
//                        player2.getName() + " (" + player2Color + ")");
//
//                // Send match found messages to both players
//                sendMatchFoundMessage(player1, gameId, player1Color, player2.getName(), game);
//                sendMatchFoundMessage(player2, gameId, player2Color, player1.getName(), game);
//
//                // Complete both players' futures
//                MatchResult result = new MatchResult(true, gameId, game);
//
//                // Complete futures in a try-catch to handle any exceptions
//                try {
//                    if (!player1.getMatchFuture().isDone()) {
//                        player1.getMatchFuture().complete(result);
//                    }
//                } catch (Exception e) {
//                    System.out.println("Error completing player1 future: " + e.getMessage());
//                }
//
//                try {
//                    if (!player2.getMatchFuture().isDone()) {
//                        player2.getMatchFuture().complete(result);
//                    }
//                } catch (Exception e) {
//                    System.out.println("Error completing player2 future: " + e.getMessage());
//                }
//
//                return result;
//            } else {
//                System.out.println("Failed to create game for matched players");
//
//                sendErrorToPlayer(player1.getSessionId(), "Failed to create game");
//                sendErrorToPlayer(player2.getSessionId(), "Failed to create game");
//
//                MatchResult failResult = new MatchResult(false, "game_creation_failed");
//                completePlayerFuture(player1, failResult);
//                completePlayerFuture(player2, failResult);
//
//
//
////                // Complete both futures with failure
////                MatchResult failResult = new MatchResult(false, "game_creation_failed");
////                player1.getMatchFuture().complete(failResult);
////                player2.getMatchFuture().complete(failResult);
//
//                return failResult;
//            }
//
//        } catch (Exception e) {
//            System.out.println("Error creating match: " + e.getMessage());
//            e.printStackTrace();
//
//            sendErrorToPlayer(player1.getSessionId(), "Match creation error");
//            sendErrorToPlayer(player2.getSessionId(), "Match creation error");
//
//            MatchResult failResult = new MatchResult(false, "match_creation_error");
//            completePlayerFuture(player1, failResult);
//            completePlayerFuture(player2, failResult);
//
//            // Complete both futures with failure
////            MatchResult failResult = new MatchResult(false, "match_creation_error");
////            if (!player1.getMatchFuture().isDone()) {
////                player1.getMatchFuture().complete(failResult);
////            }
////            if (!player2.getMatchFuture().isDone()) {
////                player2.getMatchFuture().complete(failResult);
////            }
//
//            return failResult;
//        }
//    }
//
//    private void completePlayerFuture(WaitingPlayer player, MatchResult result) {
//        try {
//            if (!player.getMatchFuture().isDone()) {
//                player.getMatchFuture().complete(result);
//            }
//        } catch (Exception e) {
//            System.out.println("Error completing future for player " + player.getName() + ": " + e.getMessage());
//        }
//    }
//
//    public void cancelMatchSearch(String sessionId) {
//        System.out.println("Cancelling match search for session: " + sessionId);
//
//        synchronized (queueLock) {
//            WaitingPlayer player = sessionToPlayer.remove(sessionId);
//            if (player != null) {
//                removePlayerFromQueue(sessionId);
//
//                // Complete the future with cancellation result
//                if (!player.getMatchFuture().isDone()) {
//                    player.getMatchFuture().complete(new MatchResult(false, "cancelled"));
//                }
//
//                // Send cancellation message
//                sendMatchCancelledMessage(sessionId, "Match search cancelled by user");
//
//                System.out.println("Match search cancelled for player: " + player.getName());
//            } else {
//                System.out.println("No active search found for session: " + sessionId);
//            }
//        }
//    }
//
//    private void removePlayerFromQueue(String sessionId) {
//        waitingQueue.removeIf(player -> player.getSessionId().equals(sessionId));
//    }
//
//    // Scheduled method to clean up expired players and handle timeouts - REDUCED FREQUENCY
//    @Scheduled(fixedRate = 15000) // Run every 15 seconds instead of 30
//    public void cleanupExpiredPlayers() {
//        List<WaitingPlayer> expiredPlayers = new ArrayList<>();
//
//        synchronized (queueLock) {
//            Iterator<WaitingPlayer> iterator = waitingQueue.iterator();
//            while (iterator.hasNext()) {
//                WaitingPlayer player = iterator.next();
//
//                if (player.isExpired()) {
//                    expiredPlayers.add(player);
//                    iterator.remove();
//                    sessionToPlayer.remove(player.getSessionId());
//                }
//            }
//        }
//
//        // Handle expired players outside the synchronized block
//        for (WaitingPlayer player : expiredPlayers) {
//            System.out.println("Player " + player.getName() + " match search timed out after " +
//                    (player.getWaitTime() / 1000) + " seconds");
//
//            // Send timeout message
//            sendMatchTimeoutMessage(player.getSessionId());
//
//            // Complete future with timeout result
//            if (!player.getMatchFuture().isDone()) {
//                player.getMatchFuture().complete(new MatchResult(false, "timeout"));
//            }
//        }
//
//        // Log queue status periodically
//        if (!waitingQueue.isEmpty()) {
//            System.out.println("Waiting queue status: " + waitingQueue.size() + " players waiting");
//            for (WaitingPlayer player : waitingQueue) {
//                System.out.println("  - " + player.getName() + " waiting for " +
//                        (player.getWaitTime() / 1000) + " seconds");
//            }
//        }
//    }
//
//    // Handle player disconnection
//    public void handlePlayerDisconnection(String sessionId) {
//        System.out.println("Handling disconnection for session: " + sessionId);
//        cancelMatchSearch(sessionId);
//    }
//
//    // WebSocket message sending methods
//    private void sendWaitingMessage(String sessionId, int queueSize) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "WAITING_FOR_OPPONENT");
//            message.put("message", "Searching for an opponent...");
//            message.put("queueSize", queueSize);
//
//            System.out.println("Sending waiting message to session: " + sessionId + " (queue size: " + queueSize + ")");
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending waiting message: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void sendErrorToPlayer(String sessionId, String error) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "MATCH_ERROR");
//            message.put("message", error);
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending error message: " + e.getMessage());
//        }
//    }
//
//    private void sendMatchFoundMessage(WaitingPlayer player, String gameId, String playerColor, String opponentName, GameSession game) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "MATCH_FOUND");
//            message.put("gameId", gameId);
//            message.put("playerColor", playerColor);
//            message.put("opponentName", opponentName);
//            message.put("gameState", game);
//
//            System.out.println("Sending match found message to: " + player.getName() + " as " + playerColor);
//            messagingTemplate.convertAndSendToUser(player.getSessionId(), "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending match found message: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private void sendMatchCancelledMessage(String sessionId, String reason) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "MATCH_CANCELLED");
//            message.put("message", reason);
//            message.put("cancelled", true);
//
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending match cancelled message: " + e.getMessage());
//        }
//    }
//
//    private void sendMatchTimeoutMessage(String sessionId) {
//        try {
//            Map<String, Object> message = new HashMap<>();
//            message.put("type", "MATCH_TIMEOUT");
//            message.put("message", "No opponent found within the time limit. Please try again.");
//            message.put("timeout", true);
//
//            System.out.println("Sending timeout message to session: " + sessionId);
//            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
//        } catch (Exception e) {
//            System.out.println("Error sending match timeout message: " + e.getMessage());
//        }
//    }
//
//    // Get current queue status
//    public Map<String, Object> getQueueStatus() {
//        Map<String, Object> status = new HashMap<>();
//        status.put("waitingPlayers", waitingQueue.size());
//        status.put("totalSessions", sessionToPlayer.size());
//        status.put("timestamp", System.currentTimeMillis());
//
//        List<Map<String, Object>> playersList = new ArrayList<>();
//        for (WaitingPlayer player : waitingQueue) {
//            Map<String, Object> playerInfo = new HashMap<>();
//            playerInfo.put("name", player.getName());
//            playerInfo.put("sessionId", player.getSessionId().substring(0, Math.min(8, player.getSessionId().length())));
//            playerInfo.put("waitTime", player.getWaitTime());
//            playerInfo.put("expired", player.isExpired());
//            playersList.add(playerInfo);
//        }
//        status.put("players", playersList);
//
//        return status;
//    }
//}

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
    private final Object queueLock = new Object();

    @Autowired
    private GameService gameService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Timeout in milliseconds (2 minutes)
    private static final long MATCH_TIMEOUT = 120000;

    @Async
    public CompletableFuture<MatchResult> findRandomMatch(String sessionId, String playerName) {
        System.out.println("=== MATCHMAKING START ===");
        System.out.println("Player " + playerName + " (" + sessionId + ") requesting random match");

        synchronized (queueLock) {
            // Remove player from queue if already waiting (prevent duplicates)
            removePlayerFromQueue(sessionId);
            WaitingPlayer existingPlayer = sessionToPlayer.remove(sessionId);
            if (existingPlayer != null) {
                System.out.println("Removed existing player from session tracking");
                // Complete the old future if it exists
                if (!existingPlayer.getMatchFuture().isDone()) {
                    existingPlayer.getMatchFuture().complete(new MatchResult(false, "replaced"));
                }
            }

            // Create waiting player
            WaitingPlayer currentPlayer = new WaitingPlayer(sessionId, playerName);
            sessionToPlayer.put(sessionId, currentPlayer);
            System.out.println("Created new waiting player: " + playerName);

            // Try to find an opponent immediately
            WaitingPlayer opponent = findAvailableOpponent(currentPlayer);

            if (opponent != null) {
                // Match found immediately
                System.out.println("IMMEDIATE MATCH: " + currentPlayer.getName() + " vs " + opponent.getName());

                // Remove both players from tracking
                sessionToPlayer.remove(opponent.getSessionId());
                sessionToPlayer.remove(currentPlayer.getSessionId());

                MatchResult result = createMatch(currentPlayer, opponent);
                return CompletableFuture.completedFuture(result);
            } else {
                // Add to waiting queue
                waitingQueue.offer(currentPlayer);
                System.out.println("QUEUED PLAYER: " + currentPlayer.getName() + " (Queue size: " + waitingQueue.size() + ")");

                // Send waiting message
                sendWaitingMessage(sessionId, waitingQueue.size());

                // Return the future that will complete when match is found or timeout occurs
                return currentPlayer.getMatchFuture();
            }
        }
    }

    private WaitingPlayer findAvailableOpponent(WaitingPlayer currentPlayer) {
        Iterator<WaitingPlayer> iterator = waitingQueue.iterator();
        while (iterator.hasNext()) {
            WaitingPlayer waitingPlayer = iterator.next();

            // Don't match with self and ensure opponent is still valid
            if (!waitingPlayer.getSessionId().equals(currentPlayer.getSessionId()) &&
                    !waitingPlayer.isExpired() &&
                    sessionToPlayer.containsKey(waitingPlayer.getSessionId())) {

                // Remove from queue
                iterator.remove();
                System.out.println("FOUND OPPONENT: " + waitingPlayer.getName() + " for " + currentPlayer.getName());
                return waitingPlayer;
            } else if (waitingPlayer.isExpired()) {
                // Clean up expired player
                iterator.remove();
                sessionToPlayer.remove(waitingPlayer.getSessionId());
                System.out.println("REMOVED EXPIRED: " + waitingPlayer.getName());
            }
        }
        return null;
    }

    private MatchResult createMatch(WaitingPlayer player1, WaitingPlayer player2) {
        try {
            System.out.println("=== CREATING MATCH ===");
            System.out.println("Players: " + player1.getName() + " vs " + player2.getName());

            // Create game using GameService
            String gameId = gameService.createGame(player1.getSessionId(), player1.getName());
            if (gameId == null) {
                System.err.println("ERROR: Failed to create game - gameId is null");
                return handleMatchCreationFailure(player1, player2, "Failed to create game");
            }

            GameSession game = gameService.joinGame(gameId, player2.getSessionId(), player2.getName());
            if (game == null) {
                System.err.println("ERROR: Failed to join game - game is null");
                return handleMatchCreationFailure(player1, player2, "Failed to join game");
            }

            // Determine colors (creator is always white)
            String player1Color = "white";
            String player2Color = "black";

            System.out.println("MATCH CREATED: Game " + gameId +
                    " - " + player1.getName() + " (" + player1Color + ") vs " +
                    player2.getName() + " (" + player2Color + ")");

            // Send match found messages to both players
            sendMatchFoundMessage(player1, gameId, player1Color, player2.getName(), game);
            sendMatchFoundMessage(player2, gameId, player2Color, player1.getName(), game);

            // Complete both players' futures
            MatchResult result = new MatchResult(true, gameId, game);
            completePlayerFuture(player1, result);
            completePlayerFuture(player2, result);

            System.out.println("=== MATCH CREATION SUCCESS ===");
            return result;

        } catch (Exception e) {
            System.err.println("ERROR in createMatch: " + e.getMessage());
            e.printStackTrace();
            return handleMatchCreationFailure(player1, player2, "Match creation error: " + e.getMessage());
        }
    }

    private MatchResult handleMatchCreationFailure(WaitingPlayer player1, WaitingPlayer player2, String errorMessage) {
        System.err.println("MATCH CREATION FAILED: " + errorMessage);

        // Send error messages to both players
        sendErrorToPlayer(player1.getSessionId(), errorMessage);
        sendErrorToPlayer(player2.getSessionId(), errorMessage);

        // Complete both futures with failure
        MatchResult failResult = new MatchResult(false, "game_creation_failed");
        completePlayerFuture(player1, failResult);
        completePlayerFuture(player2, failResult);

        return failResult;
    }

    private void completePlayerFuture(WaitingPlayer player, MatchResult result) {
        try {
            if (player != null && !player.getMatchFuture().isDone()) {
                player.getMatchFuture().complete(result);
                System.out.println("Completed future for player: " + player.getName());
            }
        } catch (Exception e) {
            System.err.println("Error completing future for player " +
                    (player != null ? player.getName() : "unknown") + ": " + e.getMessage());
        }
    }

    public void cancelMatchSearch(String sessionId) {
        System.out.println("=== CANCELLING MATCH SEARCH ===");
        System.out.println("Session: " + sessionId);

        synchronized (queueLock) {
            WaitingPlayer player = sessionToPlayer.remove(sessionId);
            if (player != null) {
                removePlayerFromQueue(sessionId);

                // Complete the future with cancellation result
                if (!player.getMatchFuture().isDone()) {
                    player.getMatchFuture().complete(new MatchResult(false, "cancelled"));
                }

                // Send cancellation message
                sendMatchCancelledMessage(sessionId, "Match search cancelled by user");

                System.out.println("CANCELLED: " + player.getName());
            } else {
                System.out.println("NO ACTIVE SEARCH: " + sessionId);
            }
        }
    }

    private void removePlayerFromQueue(String sessionId) {
        boolean removed = waitingQueue.removeIf(player -> player.getSessionId().equals(sessionId));
        if (removed) {
            System.out.println("Removed from queue: " + sessionId);
        }
    }

    // CRITICAL FIX: More frequent cleanup with proper timeout handling
    @Scheduled(fixedRate = 10000) // Run every 10 seconds
    public void cleanupExpiredPlayers() {
        long currentTime = System.currentTimeMillis();
        List<WaitingPlayer> expiredPlayers = new ArrayList<>();

        synchronized (queueLock) {
            Iterator<WaitingPlayer> iterator = waitingQueue.iterator();
            while (iterator.hasNext()) {
                WaitingPlayer player = iterator.next();

                // Check if player has been waiting too long
                if (currentTime - player.getJoinTime() >= MATCH_TIMEOUT) {
                    System.out.println("=== PLAYER TIMEOUT ===");
                    System.out.println("Player: " + player.getName() + " (waited " +
                            (currentTime - player.getJoinTime()) / 1000 + " seconds)");

                    expiredPlayers.add(player);
                    iterator.remove();
                    sessionToPlayer.remove(player.getSessionId());
                }
            }
        }

        // Handle expired players outside the synchronized block
        for (WaitingPlayer player : expiredPlayers) {
            System.out.println("TIMING OUT: " + player.getName());

            // Send timeout message
            sendMatchTimeoutMessage(player.getSessionId());

            // Complete future with timeout result
            if (!player.getMatchFuture().isDone()) {
                player.getMatchFuture().complete(new MatchResult(false, "timeout"));
            }
        }

        // Log queue status if not empty
        if (!waitingQueue.isEmpty()) {
            System.out.println("=== QUEUE STATUS ===");
            System.out.println("Waiting players: " + waitingQueue.size());
            for (WaitingPlayer player : waitingQueue) {
                long waitTime = (currentTime - player.getJoinTime()) / 1000;
                System.out.println("  - " + player.getName() + " waiting " + waitTime + "s");
            }
            System.out.println("=================");
        }
    }

    // Handle player disconnection
    public void handlePlayerDisconnection(String sessionId) {
        System.out.println("=== PLAYER DISCONNECTION ===");
        System.out.println("Session: " + sessionId);
        cancelMatchSearch(sessionId);
    }

    // WebSocket message sending methods
    private void sendWaitingMessage(String sessionId, int queueSize) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "WAITING_FOR_OPPONENT");
            message.put("message", "Searching for an opponent...");
            message.put("queueSize", queueSize);

            System.out.println("SENDING WAITING MESSAGE: " + sessionId + " (queue: " + queueSize + ")");
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
        } catch (Exception e) {
            System.err.println("Error sending waiting message to " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendErrorToPlayer(String sessionId, String error) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "MATCH_ERROR");
            message.put("message", error);
            System.out.println("SENDING ERROR: " + sessionId + " - " + error);
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
        } catch (Exception e) {
            System.err.println("Error sending error message to " + sessionId + ": " + e.getMessage());
        }
    }

    private void sendMatchFoundMessage(WaitingPlayer player, String gameId, String playerColor, String opponentName, GameSession game) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "MATCH_FOUND");
            message.put("gameId", gameId);
            message.put("playerColor", playerColor);
            message.put("opponentName", opponentName);
            message.put("gameState", game);

            System.out.println("SENDING MATCH FOUND: " + player.getName() + " as " + playerColor);
            messagingTemplate.convertAndSendToUser(player.getSessionId(), "/queue/match", message);
        } catch (Exception e) {
            System.err.println("Error sending match found message to " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMatchCancelledMessage(String sessionId, String reason) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "MATCH_CANCELLED");
            message.put("message", reason);
            message.put("cancelled", true);

            System.out.println("SENDING CANCELLED: " + sessionId);
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
        } catch (Exception e) {
            System.err.println("Error sending cancelled message to " + sessionId + ": " + e.getMessage());
        }
    }

    private void sendMatchTimeoutMessage(String sessionId) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "MATCH_TIMEOUT");
            message.put("message", "No opponent found within the time limit. Please try again.");
            message.put("timeout", true);

            System.out.println("SENDING TIMEOUT: " + sessionId);
            messagingTemplate.convertAndSendToUser(sessionId, "/queue/match", message);
        } catch (Exception e) {
            System.err.println("Error sending timeout message to " + sessionId + ": " + e.getMessage());
        }
    }

    // Get current queue status
    public Map<String, Object> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("waitingPlayers", waitingQueue.size());
        status.put("totalSessions", sessionToPlayer.size());
        status.put("timestamp", System.currentTimeMillis());

        List<Map<String, Object>> playersList = new ArrayList<>();
        for (WaitingPlayer player : waitingQueue) {
            Map<String, Object> playerInfo = new HashMap<>();
            playerInfo.put("name", player.getName());
            playerInfo.put("sessionId", player.getSessionId().substring(0, Math.min(8, player.getSessionId().length())));
            playerInfo.put("waitTime", player.getWaitTime());
            playerInfo.put("expired", player.isExpired());
            playersList.add(playerInfo);
        }
        status.put("players", playersList);

        return status;
    }
}