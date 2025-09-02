package com.chess.multiplayer.controller;

import com.chess.multiplayer.model.GameMessage;
import com.chess.multiplayer.model.GameSession;
import com.chess.multiplayer.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// RestController for HTTP endpoints
@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "*")
class GameRestController {

    @Autowired
    private GameService gameService;

    @PostMapping("/create")
    public GameSession createGame(@RequestBody GameMessage request) {
        String gameId = gameService.createGame(request.getPlayerId(), request.getPlayerName());
        return gameService.getGame(gameId);
    }

    @PostMapping("/join-random")
    public GameSession joinRandomGame(@RequestBody GameMessage request) {
        return gameService.joinRandomGame(request.getPlayerId(), request.getPlayerName());
    }

    @GetMapping("/active")
    public List<GameSession> getActiveGames() {
        return gameService.getActiveGames();
    }

    @GetMapping("/{gameId}")
    public GameSession getGame(@PathVariable String gameId) {
        return gameService.getGame(gameId);
    }

    @GetMapping("/player/{playerId}")
    public GameSession getPlayerGame(@PathVariable String playerId) {
        String gameId = gameService.getPlayerGame(playerId);
        return gameId != null ? gameService.getGame(gameId) : null;
    }
}