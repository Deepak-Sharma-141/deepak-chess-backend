// GameMessage.java (for WebSocket communication)
package com.chess.multiplayer.model;

public class GameMessage {
    private String type; // "move", "join", "leave", "chat", "gameState"
    private String gameId;
    private String playerId;
    private String playerName;
    private Move move;
    private String message;
    private GameSession gameState;
    private String error;

    public GameMessage() {}

    public GameMessage(String type, String gameId) {
        this.type = type;
        this.gameId = gameId;
    }

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public Move getMove() { return move; }
    public void setMove(Move move) { this.move = move; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public GameSession getGameState() { return gameState; }
    public void setGameState(GameSession gameState) { this.gameState = gameState; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}