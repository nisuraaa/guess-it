package com.indezah.guess_it.dtos;

public class WebSocketMessageDTO {
    public String action;
    public String secretNumber;
    public String roomCode;
    public String playerCode;
    public String guessedNumber;

    public WebSocketMessageDTO() {
    }

    public WebSocketMessageDTO(String action, String secretNumber, String roomCode, String playerCode) {
        this.action = action;
        this.secretNumber = secretNumber;
        this.roomCode = roomCode;
        this.playerCode = playerCode;
    }

    public WebSocketMessageDTO(String action, String secretNumber, String roomCode, String playerCode, String guessedNumber) {
        this.action = action;
        this.secretNumber = secretNumber;
        this.roomCode = roomCode;
        this.playerCode = playerCode;
        this.guessedNumber = guessedNumber;
    }
}