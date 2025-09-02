package com.chess.multiplayer.model;

// GameSession.java
//package com.chess.multiplayer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GameSession {
    private String gameId;
    private Player whitePlayer;
    private Player blackPlayer;
    private String currentTurn; // "white" or "black"
    private String gameStatus; // "waiting", "active", "finished"
    private List<Move> moveHistory;
    private String boardState; // JSON representation of board
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String winner; // null, "white", "black", "draw"

    public GameSession() {
        this.moveHistory = new ArrayList<>();
        this.currentTurn = "white";
        this.gameStatus = "waiting";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public GameSession(String gameId) {
        this();
        this.gameId = gameId;
    }

    public boolean isGameFull() {
        return whitePlayer != null && blackPlayer != null;
    }

    public Player getOpponent(String playerId) {
        if (whitePlayer != null && whitePlayer.getId().equals(playerId)) {
            return blackPlayer;
        } else if (blackPlayer != null && blackPlayer.getId().equals(playerId)) {
            return whitePlayer;
        }
        return null;
    }

    public Player getPlayerByColor(String color) {
        if ("white".equals(color)) {
            return whitePlayer;
        } else if ("black".equals(color)) {
            return blackPlayer;
        }
        return null;
    }

    // Getters and setters
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public Player getWhitePlayer() { return whitePlayer; }
    public void setWhitePlayer(Player whitePlayer) {
        this.whitePlayer = whitePlayer;
        if (whitePlayer != null) {
            whitePlayer.setColor("white");
        }
    }

    public Player getBlackPlayer() { return blackPlayer; }
    public void setBlackPlayer(Player blackPlayer) {
        this.blackPlayer = blackPlayer;
        if (blackPlayer != null) {
            blackPlayer.setColor("black");
        }
    }

    public String getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(String currentTurn) { this.currentTurn = currentTurn; }

    public String getGameStatus() { return gameStatus; }
    public void setGameStatus(String gameStatus) { this.gameStatus = gameStatus; }

    public List<Move> getMoveHistory() { return moveHistory; }
    public void setMoveHistory(List<Move> moveHistory) { this.moveHistory = moveHistory; }

    public String getBoardState() { return boardState; }
    public void setBoardState(String boardState) { this.boardState = boardState; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getWinner() { return winner; }
    public void setWinner(String winner) { this.winner = winner; }
}