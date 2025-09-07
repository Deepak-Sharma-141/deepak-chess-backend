package com.chess.multiplayer.model;

import java.util.concurrent.CompletableFuture;

public class WaitingPlayer {
    private String sessionId;
    private String name;
    private long joinTime;
    private CompletableFuture<MatchResult> matchFuture;
    private static final long TIMEOUT_MS = 120000; // 2 minutes

    public WaitingPlayer(String sessionId, String name) {
        this.sessionId = sessionId;
        this.name = name;
        this.joinTime = System.currentTimeMillis();
        this.matchFuture = new CompletableFuture<>();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - joinTime > TIMEOUT_MS;
    }

    public long getWaitTime() {
        return System.currentTimeMillis() - joinTime;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getName() { return name; }
    public long getJoinTime() { return joinTime; }
    public CompletableFuture<MatchResult> getMatchFuture() { return matchFuture; }
}

