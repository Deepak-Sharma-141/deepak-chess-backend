// Move.java
package com.chess.multiplayer.model;

import java.time.LocalDateTime;

public class Move {
    private String moveId;
    private String playerId;
    private String playerColor;
    private int fromRow;
    private int fromCol;
    private int toRow;
    private int toCol;
    private String piece;
    private String capturedPiece;
    private String notation;
    private String promotedTo;
    private boolean isEnPassant;
    private boolean isCheck;
    private boolean isCheckmate;
    private LocalDateTime timestamp;

    public Move() {
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public String getMoveId() { return moveId; }
    public void setMoveId(String moveId) { this.moveId = moveId; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerColor() { return playerColor; }
    public void setPlayerColor(String playerColor) { this.playerColor = playerColor; }

    public int getFromRow() { return fromRow; }
    public void setFromRow(int fromRow) { this.fromRow = fromRow; }

    public int getFromCol() { return fromCol; }
    public void setFromCol(int fromCol) { this.fromCol = fromCol; }

    public int getToRow() { return toRow; }
    public void setToRow(int toRow) { this.toRow = toRow; }

    public int getToCol() { return toCol; }
    public void setToCol(int toCol) { this.toCol = toCol; }

    public String getPiece() { return piece; }
    public void setPiece(String piece) { this.piece = piece; }

    public String getCapturedPiece() { return capturedPiece; }
    public void setCapturedPiece(String capturedPiece) { this.capturedPiece = capturedPiece; }

    public String getNotation() { return notation; }
    public void setNotation(String notation) { this.notation = notation; }

    public String getPromotedTo() { return promotedTo; }
    public void setPromotedTo(String promotedTo) { this.promotedTo = promotedTo; }

    public boolean isEnPassant() { return isEnPassant; }
    public void setEnPassant(boolean enPassant) { isEnPassant = enPassant; }

    public boolean isCheck() { return isCheck; }
    public void setCheck(boolean check) { isCheck = check; }

    public boolean isCheckmate() { return isCheckmate; }
    public void setCheckmate(boolean checkmate) { isCheckmate = checkmate; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
