package com.chess.multiplayer.model;

public class MatchResult {
    private boolean success;
    private String gameId;
    private GameSession game;
    private String reason;

    public MatchResult(boolean success) {
        this.success = success;
    }

    public MatchResult(boolean success, String reason) {
        this.success = success;
        this.reason = reason;
    }

    public MatchResult(boolean success, String gameId, GameSession game) {
        this.success = success;
        this.gameId = gameId;
        this.game = game;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getGameId() { return gameId; }
    public GameSession getGame() { return game; }
    public String getReason() { return reason; }
}