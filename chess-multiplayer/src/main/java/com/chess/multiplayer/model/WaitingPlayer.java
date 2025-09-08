//package com.chess.multiplayer.model;
//
//import java.util.concurrent.CompletableFuture;
//
//public class WaitingPlayer {
//    private String sessionId;
//    private String name;
//    private long joinTime;
//    private CompletableFuture<MatchResult> matchFuture;
//    private static final long TIMEOUT_MS = 120000; // 2 minutes
//
//    public WaitingPlayer(String sessionId, String name) {
//        this.sessionId = sessionId;
//        this.name = name;
//        this.joinTime = System.currentTimeMillis();
//        this.matchFuture = new CompletableFuture<>();
//    }
//
//    public boolean isExpired() {
//        return System.currentTimeMillis() - joinTime > TIMEOUT_MS;
//    }
//
//    public long getWaitTime() {
//        return System.currentTimeMillis() - joinTime;
//    }
//
//    // Getters
//    public String getSessionId() { return sessionId; }
//    public String getName() { return name; }
//    public long getJoinTime() { return joinTime; }
//    public CompletableFuture<MatchResult> getMatchFuture() { return matchFuture; }
//}
//

package com.chess.multiplayer.model;

import java.util.concurrent.CompletableFuture;

public class WaitingPlayer {
    private String sessionId;
    private String name;
    private long joinTime;
    private CompletableFuture<MatchResult> matchFuture;

    // Timeout set to exactly 2 minutes (120 seconds)
    private static final long TIMEOUT_MS = 120000; // 2 minutes

    public WaitingPlayer(String sessionId, String name) {
        this.sessionId = sessionId;
        this.name = name;
        this.joinTime = System.currentTimeMillis();
        this.matchFuture = new CompletableFuture<>();

        System.out.println("Created WaitingPlayer: " + name + " at " + joinTime);
    }

    public boolean isExpired() {
        long currentTime = System.currentTimeMillis();
        long waitTime = currentTime - joinTime;
        boolean expired = waitTime >= TIMEOUT_MS;

        if (expired) {
            System.out.println("Player " + name + " EXPIRED after " + (waitTime / 1000) + " seconds");
        }

        return expired;
    }

    public long getWaitTime() {
        return System.currentTimeMillis() - joinTime;
    }

    public long getWaitTimeSeconds() {
        return getWaitTime() / 1000;
    }

    public long getRemainingTime() {
        long elapsed = System.currentTimeMillis() - joinTime;
        return Math.max(0, TIMEOUT_MS - elapsed);
    }

    public long getRemainingTimeSeconds() {
        return getRemainingTime() / 1000;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getName() { return name; }
    public long getJoinTime() { return joinTime; }
    public CompletableFuture<MatchResult> getMatchFuture() { return matchFuture; }
    public static long getTimeoutMs() { return TIMEOUT_MS; }
}
