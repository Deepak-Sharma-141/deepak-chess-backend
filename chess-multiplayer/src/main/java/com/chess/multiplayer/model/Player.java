package com.chess.multiplayer.model;

import java.time.LocalDateTime;

public class Player {
    private String id;           // Session ID or player ID
    private String name;         // Player's display name
    private String color;        // "white" or "black"
    private boolean connected;   // Connection status
    private LocalDateTime joinTime;  // When player joined
    private LocalDateTime lastActivity; // Last activity time

    // Default constructor
    public Player() {
        this.connected = true;
        this.joinTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }

    // Constructor with ID and name
    public Player(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    // Constructor with ID, name, and color
    public Player(String id, String name, String color) {
        this(id, name);
        this.color = color;
    }

    // Update last activity timestamp
    public void updateActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    // Check if player has been inactive for too long
    public boolean isInactive(long timeoutMinutes) {
        if (lastActivity == null) return false;
        return lastActivity.isBefore(LocalDateTime.now().minusMinutes(timeoutMinutes));
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
        if (connected) {
            updateActivity();
        }
    }

    public LocalDateTime getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }

    // Utility methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Player player = (Player) obj;
        return id != null && id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Player{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", color='" + color + '\'' +
                ", connected=" + connected +
                ", joinTime=" + joinTime +
                ", lastActivity=" + lastActivity +
                '}';
    }
}