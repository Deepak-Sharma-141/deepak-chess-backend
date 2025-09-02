// GameService.java
package com.chess.multiplayer.service;

import com.chess.multiplayer.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {
    private final Map<String, GameSession> games = new ConcurrentHashMap<>();
    private final Map<String, String> playerToGame = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String createGame(String playerId, String playerName) {
        String gameId = generateGameId();
        GameSession game = new GameSession(gameId);

        Player player = new Player(playerId, playerName);
        game.setWhitePlayer(player);
        game.setBoardState(initializeBoardState());
        game.setGameStatus("waiting");

        games.put(gameId, game);
        playerToGame.put(playerId, gameId);

        return gameId;
    }

    public GameSession joinGame(String gameId, String playerId, String playerName) {
        GameSession game = games.get(gameId);
        if (game == null) return null;

        Player player = new Player(playerId, playerName);

        if (game.getWhitePlayer() == null) {
            game.setWhitePlayer(player);
        } else if (game.getBlackPlayer() == null) {
            game.setBlackPlayer(player);
            game.setGameStatus("active");
        } else {
            return null; // Game is full
        }

        playerToGame.put(playerId, gameId);
        game.setUpdatedAt(LocalDateTime.now());
        return game;
    }

    public GameSession joinRandomGame(String playerId, String playerName) {
        // Find a waiting game
        for (GameSession game : games.values()) {
            if ("waiting".equals(game.getGameStatus()) && !game.isGameFull()) {
                return joinGame(game.getGameId(), playerId, playerName);
            }
        }
        return null;
    }

    public boolean makeMove(String gameId, String playerId, Move move) {
        GameSession game = games.get(gameId);
        if (game == null) return false;

        // Validate it's player's turn
        Player currentPlayer = game.getPlayerByColor(game.getCurrentTurn());
        if (currentPlayer == null || !currentPlayer.getId().equals(playerId)) {
            return false;
        }

        // Set move details
        move.setMoveId(UUID.randomUUID().toString());
        move.setPlayerId(playerId);
        move.setPlayerColor(game.getCurrentTurn());
        move.setTimestamp(LocalDateTime.now());

        // Add move to history
        game.getMoveHistory().add(move);

        // Switch turns
        game.setCurrentTurn(game.getCurrentTurn().equals("white") ? "black" : "white");
        game.setUpdatedAt(LocalDateTime.now());

        // Update board state (you'll need to implement board logic here)
        updateBoardState(game, move);

        return true;
    }

    private void updateBoardState(GameSession game, Move move) {
        // For now, just update the timestamp
        // You would implement the actual chess board logic here
        // This should apply the move to the board state JSON
        game.setUpdatedAt(LocalDateTime.now());
    }

    public GameSession getGame(String gameId) {
        return games.get(gameId);
    }

    public String getPlayerGame(String playerId) {
        return playerToGame.get(playerId);
    }

    public boolean isPlayerInGame(String playerId, String gameId) {
        return gameId.equals(playerToGame.get(playerId));
    }

    public void disconnectPlayer(String playerId) {
        String gameId = playerToGame.remove(playerId);
        if (gameId != null) {
            GameSession game = games.get(gameId);
            if (game != null) {
                // Mark player as disconnected
                if (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(playerId)) {
                    game.getWhitePlayer().setConnected(false);
                }
                if (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(playerId)) {
                    game.getBlackPlayer().setConnected(false);
                }

                // If both players disconnected, remove game
                boolean whiteConnected = game.getWhitePlayer() != null && game.getWhitePlayer().isConnected();
                boolean blackConnected = game.getBlackPlayer() != null && game.getBlackPlayer().isConnected();

                if (!whiteConnected && !blackConnected) {
                    games.remove(gameId);
                }
            }
        }
    }

    public List<GameSession> getActiveGames() {
        return new ArrayList<>(games.values());
    }

    private String generateGameId() {
        return "GAME-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String initializeBoardState() {
        // Initialize a standard chess board
        Map<String, Object> boardState = new HashMap<>();
        String[][] board = new String[8][8];

        // Initialize empty board
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                board[i][j] = null;
            }
        }

        // Place pieces
        String[] backRow = {"rook", "knight", "bishop", "queen", "king", "bishop", "knight", "rook"};

        // Black pieces
        for (int i = 0; i < 8; i++) {
            board[0][i] = "black_" + backRow[i];
            board[1][i] = "black_pawn";
        }

        // White pieces
        for (int i = 0; i < 8; i++) {
            board[6][i] = "white_pawn";
            board[7][i] = "white_" + backRow[i];
        }

        boardState.put("board", board);
        boardState.put("currentPlayer", "white");

        try {
            return objectMapper.writeValueAsString(boardState);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}