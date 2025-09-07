package com.chess.multiplayer.controller;

import com.chess.multiplayer.service.MatchmakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// REST endpoints for matchmaking status and admin functions
@RestController
@RequestMapping("/api/matchmaking")
@CrossOrigin(origins = "*")
class MatchmakingRestController {

    @Autowired
    private MatchmakingService matchmakingService;

    @GetMapping("/status")
    public Map<String, Object> getMatchmakingStatus() {
        return matchmakingService.getQueueStatus();
    }

    @PostMapping("/cancel/{sessionId}")
    public Map<String, Object> cancelMatchmaking(@PathVariable String sessionId) {
        matchmakingService.cancelMatchSearch(sessionId);
        return Map.of("success", true, "message", "Match search cancelled");
    }
}