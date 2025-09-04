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
        if (game == null || !"active".equals(game.getGameStatus())) {
            return false;
        }

        // Validate it's player's turn
        Player currentPlayer = game.getPlayerByColor(game.getCurrentTurn());
        if (currentPlayer == null || !currentPlayer.getId().equals(playerId)) {
            return false;
        }

        // Validate the move
        if (!isValidMove(game, move)) {
            return false;
        }

        // Set move details
        move.setMoveId(UUID.randomUUID().toString());
        move.setPlayerId(playerId);
        move.setPlayerColor(game.getCurrentTurn());
        move.setTimestamp(LocalDateTime.now());

        // Add move to history
        game.getMoveHistory().add(move);

        // Clear any pending draw offers when a move is made
        game.clearDrawOffer();

        // Switch turns
        game.setCurrentTurn(game.getCurrentTurn().equals("white") ? "black" : "white");
        game.setUpdatedAt(LocalDateTime.now());

        // Update board state
        updateBoardState(game, move);

        // Check for game end conditions (checkmate, stalemate)
        checkGameEndConditions(game);

        return true;
    }

    private boolean isValidMove(GameSession game, Move move) {
        try {
            // Parse current board state
            Map<String, Object> boardState = objectMapper.readValue(game.getBoardState(), Map.class);
            String[][] board = (String[][]) boardState.get("board");
            
            // Basic validation
            if (move.getFromRow() < 0 || move.getFromRow() > 7 || 
                move.getFromCol() < 0 || move.getFromCol() > 7 ||
                move.getToRow() < 0 || move.getToRow() > 7 || 
                move.getToCol() < 0 || move.getToCol() > 7) {
                return false;
            }
            
            // Check if there's a piece at the source
            String piece = board[move.getFromRow()][move.getFromCol()];
            if (piece == null) {
                return false;
            }
            
            // Check if it's the player's piece
            String pieceColor = piece.split("_")[0];
            if (!pieceColor.equals(game.getCurrentTurn())) {
                return false;
            }
            
            // Check if destination is not occupied by own piece
            String destinationPiece = board[move.getToRow()][move.getToCol()];
            if (destinationPiece != null && destinationPiece.split("_")[0].equals(pieceColor)) {
                return false;
            }
            
            // Basic move validation (you can expand this with full chess rules)
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }

    private void checkGameEndConditions(GameSession game) {
        // This is a simplified check - you would implement full chess logic here
        // For now, we'll just check if the game has been going on for too long
        // In a real implementation, you'd check for checkmate, stalemate, etc.
        
        if (game.getMoveHistory().size() > 1000) { // Arbitrary limit
            game.setGameStatus("finished");
            game.setWinner("draw");
        }
    }

    private void updateBoardState(GameSession game, Move move) {
        try {
            // Parse current board state
            Map<String, Object> boardState = objectMapper.readValue(game.getBoardState(), Map.class);
            String[][] board = (String[][]) boardState.get("board");
            
            // Apply the move
            String piece = board[move.getFromRow()][move.getFromCol()];
            board[move.getToRow()][move.getToCol()] = piece;
            board[move.getFromRow()][move.getFromCol()] = null;
            
            // Handle special moves (castling, en passant, promotion)
            if (move.getPromotedTo() != null) {
                String color = piece.split("_")[0];
                board[move.getToRow()][move.getToCol()] = color + "_" + move.getPromotedTo();
            }
            
            // Update board state
            boardState.put("board", board);
            boardState.put("currentPlayer", game.getCurrentTurn());
            
            // Update the game's board state
            game.setBoardState(objectMapper.writeValueAsString(boardState));
            game.setUpdatedAt(LocalDateTime.now());
            
        } catch (Exception e) {
            // If there's an error parsing/updating board state, just update timestamp
            game.setUpdatedAt(LocalDateTime.now());
        }
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

    public GameSession resignGame(String gameId, String playerId) {
        GameSession game = games.get(gameId);
        if (game == null || !"active".equals(game.getGameStatus())) {
            return null;
        }

        // Determine the winner (opponent of the resigning player)
        String winner;
        if (game.getWhitePlayer() != null && game.getWhitePlayer().getId().equals(playerId)) {
            winner = "black";
        } else if (game.getBlackPlayer() != null && game.getBlackPlayer().getId().equals(playerId)) {
            winner = "white";
        } else {
            return null; // Player not in this game
        }

        // Update game state
        game.setGameStatus("finished");
        game.setWinner(winner);
        game.setUpdatedAt(LocalDateTime.now());

        return game;
    }

    public GameSession offerDraw(String gameId, String playerId) {
        GameSession game = games.get(gameId);
        if (game == null || !"active".equals(game.getGameStatus())) {
            return null;
        }

        // Check if player is in the game
        if (!isPlayerInGame(playerId, gameId)) {
            return null;
        }

        // Set draw offer
        game.setDrawOfferBy(playerId);
        game.setDrawOfferTime(LocalDateTime.now());
        game.setUpdatedAt(LocalDateTime.now());

        return game;
    }

    public GameSession acceptDraw(String gameId, String playerId) {
        GameSession game = games.get(gameId);
        if (game == null || !"active".equals(game.getGameStatus())) {
            return null;
        }

        // Check if there's a draw offer and it's not from the same player
        if (!game.hasDrawOffer() || game.getDrawOfferBy().equals(playerId)) {
            return null;
        }

        // Check if player is in the game
        if (!isPlayerInGame(playerId, gameId)) {
            return null;
        }

        // Accept the draw
        game.setGameStatus("finished");
        game.setWinner("draw");
        game.clearDrawOffer();
        game.setUpdatedAt(LocalDateTime.now());

        return game;
    }

    public GameSession declineDraw(String gameId, String playerId) {
        GameSession game = games.get(gameId);
        if (game == null || !"active".equals(game.getGameStatus())) {
            return null;
        }

        // Check if there's a draw offer and it's not from the same player
        if (!game.hasDrawOffer() || game.getDrawOfferBy().equals(playerId)) {
            return null;
        }

        // Check if player is in the game
        if (!isPlayerInGame(playerId, gameId)) {
            return null;
        }

        // Decline the draw (clear the offer)
        game.clearDrawOffer();
        game.setUpdatedAt(LocalDateTime.now());

        return game;
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