// GameController.java
package com.chess.multiplayer.controller;

import com.chess.multiplayer.model.GameMessage;
import com.chess.multiplayer.model.GameSession;
import com.chess.multiplayer.model.Move;
import com.chess.multiplayer.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class GameController {

    @Autowired
    private GameService gameService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/game/{gameId}/join")
    public void joinGame(@DestinationVariable String gameId, GameMessage message) {
        try {
            GameSession game = gameService.joinGame(gameId, message.getPlayerId(), message.getPlayerName());

            if (game != null) {
                // Send game state to the player
                GameMessage response = new GameMessage("gameJoined", gameId);
                response.setGameState(game);
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), response);

                // Notify all players in the game
                GameMessage notification = new GameMessage("playerJoined", gameId);
                notification.setPlayerName(message.getPlayerName());
                notification.setGameState(game);
                messagingTemplate.convertAndSend("/topic/game/" + gameId, notification);

                // If game is now full, notify that it's starting
                if (game.isGameFull() && "active".equals(game.getGameStatus())) {
                    GameMessage gameStart = new GameMessage("gameStart", gameId);
                    gameStart.setGameState(game);
                    messagingTemplate.convertAndSend("/topic/game/" + gameId, gameStart);
                }
            } else {
                // Send error message
                GameMessage error = new GameMessage("error", gameId);
                error.setError("Unable to join game. Game may be full or not exist.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
            }
        } catch (Exception e) {
            GameMessage error = new GameMessage("error", gameId);
            error.setError("An error occurred while joining the game: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
        }
    }

    @MessageMapping("/game/{gameId}/move")
    public void makeMove(@DestinationVariable String gameId, GameMessage message) {
        try {
            if (!gameService.isPlayerInGame(message.getPlayerId(), gameId)) {
                GameMessage error = new GameMessage("error", gameId);
                error.setError("You are not authorized to make moves in this game.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
                return;
            }

            boolean moveSuccessful = gameService.makeMove(gameId, message.getPlayerId(), message.getMove());

            if (moveSuccessful) {
                GameSession updatedGame = gameService.getGame(gameId);

                // Broadcast move to all players in the game
                GameMessage moveNotification = new GameMessage("move", gameId);
                moveNotification.setMove(message.getMove());
                moveNotification.setGameState(updatedGame);
                messagingTemplate.convertAndSend("/topic/game/" + gameId, moveNotification);

                // Check if game ended
                if ("finished".equals(updatedGame.getGameStatus())) {
                    GameMessage gameEnd = new GameMessage("gameEnd", gameId);
                    gameEnd.setGameState(updatedGame);
                    messagingTemplate.convertAndSend("/topic/game/" + gameId, gameEnd);
                }
            } else {
                GameMessage error = new GameMessage("error", gameId);
                error.setError("Invalid move or not your turn.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
            }
        } catch (Exception e) {
            GameMessage error = new GameMessage("error", gameId);
            error.setError("An error occurred while making the move: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
        }
    }

    @MessageMapping("/game/{gameId}/disconnect")
    public void disconnectPlayer(@DestinationVariable String gameId, GameMessage message) {
        gameService.disconnectPlayer(message.getPlayerId());

        // Notify other players
        GameMessage notification = new GameMessage("playerDisconnected", gameId);
        notification.setPlayerName(message.getPlayerName());
        messagingTemplate.convertAndSend("/topic/game/" + gameId, notification);
    }

    @SubscribeMapping("/game/{gameId}")
    public GameMessage getGameState(@DestinationVariable String gameId) {
        GameSession game = gameService.getGame(gameId);
        GameMessage response = new GameMessage("gameState", gameId);
        response.setGameState(game);
        return response;
    }
}

