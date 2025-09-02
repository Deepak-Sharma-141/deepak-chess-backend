package com.chess.multiplayer.model;

// Player.java
//package com.chess.multiplayer.model;

public class Player {
    private String id;
    private String name;
    private String color; // "white" or "black"
    private boolean connected;

    public Player() {}

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.connected = true;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }
}
