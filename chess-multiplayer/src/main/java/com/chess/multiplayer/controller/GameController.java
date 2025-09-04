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
            System.out.println("Received move request for game: " + gameId);
            System.out.println("Player ID: " + message.getPlayerId());
            System.out.println("Move: " + (message.getMove() != null ? 
                "fromRow=" + message.getMove().getFromRow() + 
                ", fromCol=" + message.getMove().getFromCol() + 
                ", toRow=" + message.getMove().getToRow() + 
                ", toCol=" + message.getMove().getToCol() : "null"));

            if (!gameService.isPlayerInGame(message.getPlayerId(), gameId)) {
                System.out.println("Player not in game: " + message.getPlayerId());
                GameMessage error = new GameMessage("error", gameId);
                error.setError("You are not authorized to make moves in this game.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
                return;
            }

            boolean moveSuccessful = gameService.makeMove(gameId, message.getPlayerId(), message.getMove());
            System.out.println("Move successful: " + moveSuccessful);

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
                System.out.println("Move failed - sending error");
                GameMessage error = new GameMessage("error", gameId);
                error.setError("Invalid move or not your turn.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
            }
        } catch (Exception e) {
            System.out.println("Exception in makeMove: " + e.getMessage());
            e.printStackTrace();
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

    @MessageMapping("/game/{gameId}/resign")
    public void resignGame(@DestinationVariable String gameId, GameMessage message) {
        try {
            if (!gameService.isPlayerInGame(message.getPlayerId(), gameId)) {
                GameMessage error = new GameMessage("error", gameId);
                error.setError("You are not authorized to resign from this game.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
                return;
            }

            GameSession updatedGame = gameService.resignGame(gameId, message.getPlayerId());

            if (updatedGame != null) {
                // Broadcast resignation to all players
                GameMessage resignationNotification = new GameMessage("resign", gameId);
                resignationNotification.setPlayerId(message.getPlayerId());
                resignationNotification.setPlayerName(message.getPlayerName());
                resignationNotification.setGameState(updatedGame);
                messagingTemplate.convertAndSend("/topic/game/" + gameId, resignationNotification);

                // Send game end notification
                GameMessage gameEnd = new GameMessage("gameEnd", gameId);
                gameEnd.setGameState(updatedGame);
                messagingTemplate.convertAndSend("/topic/game/" + gameId, gameEnd);
            }
        } catch (Exception e) {
            GameMessage error = new GameMessage("error", gameId);
            error.setError("An error occurred while resigning: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
        }
    }

    @MessageMapping("/game/{gameId}/draw-offer")
    public void offerDraw(@DestinationVariable String gameId, GameMessage message) {
        try {
            if (!gameService.isPlayerInGame(message.getPlayerId(), gameId)) {
                GameMessage error = new GameMessage("error", gameId);
                error.setError("You are not authorized to offer a draw in this game.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
                return;
            }

            GameSession updatedGame = gameService.offerDraw(gameId, message.getPlayerId());

            if (updatedGame != null) {
                // Broadcast draw offer to all players
                GameMessage drawOfferNotification = new GameMessage("drawOffer", gameId);
                drawOfferNotification.setPlayerId(message.getPlayerId());
                drawOfferNotification.setPlayerName(message.getPlayerName());
                drawOfferNotification.setGameState(updatedGame);
                messagingTemplate.convertAndSend("/topic/game/" + gameId, drawOfferNotification);
            }
        } catch (Exception e) {
            GameMessage error = new GameMessage("error", gameId);
            error.setError("An error occurred while offering a draw: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
        }
    }

    @MessageMapping("/game/{gameId}/draw-accept")
    public void acceptDraw(@DestinationVariable String gameId, GameMessage message) {
        try {
            if (!gameService.isPlayerInGame(message.getPlayerId(), gameId)) {
                GameMessage error = new GameMessage("error", gameId);
                error.setError("You are not authorized to accept a draw in this game.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
                return;
            }

            GameSession updatedGame = gameService.acceptDraw(gameId, message.getPlayerId());

            if (updatedGame != null) {
                // Broadcast draw acceptance to all players
                GameMessage drawAcceptNotification = new GameMessage("drawAccept", gameId);
                drawAcceptNotification.setPlayerId(message.getPlayerId());
                drawAcceptNotification.setPlayerName(message.getPlayerName());
                drawAcceptNotification.setGameState(updatedGame);
                messagingTemplate.convertAndSend("/topic/game/" + gameId, drawAcceptNotification);

                // Send game end notification
                GameMessage gameEnd = new GameMessage("gameEnd", gameId);
                gameEnd.setGameState(updatedGame);
                messagingTemplate.convertAndSend("/topic/game/" + gameId, gameEnd);
            }
        } catch (Exception e) {
            GameMessage error = new GameMessage("error", gameId);
            error.setError("An error occurred while accepting the draw: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
        }
    }

    @MessageMapping("/game/{gameId}/draw-decline")
    public void declineDraw(@DestinationVariable String gameId, GameMessage message) {
        try {
            if (!gameService.isPlayerInGame(message.getPlayerId(), gameId)) {
                GameMessage error = new GameMessage("error", gameId);
                error.setError("You are not authorized to decline a draw in this game.");
                messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
                return;
            }

            GameSession updatedGame = gameService.declineDraw(gameId, message.getPlayerId());

            if (updatedGame != null) {
                // Broadcast draw decline to all players
                GameMessage drawDeclineNotification = new GameMessage("drawDecline", gameId);
                drawDeclineNotification.setPlayerId(message.getPlayerId());
                drawDeclineNotification.setPlayerName(message.getPlayerName());
                drawDeclineNotification.setGameState(updatedGame);
                messagingTemplate.convertAndSend("/topic/game/" + gameId, drawDeclineNotification);
            }
        } catch (Exception e) {
            GameMessage error = new GameMessage("error", gameId);
            error.setError("An error occurred while declining the draw: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/player/" + message.getPlayerId(), error);
        }
    }

    @SubscribeMapping("/game/{gameId}")
    public GameMessage getGameState(@DestinationVariable String gameId) {
        GameSession game = gameService.getGame(gameId);
        GameMessage response = new GameMessage("gameState", gameId);
        response.setGameState(game);
        return response;
    }
}

