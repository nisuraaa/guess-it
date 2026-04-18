package com.indezah.guess_it.dtos;

public class SetSecretNumberResponseDTO {
    private String playerTurn;
    private boolean secretNumberReady;
    private String[] players;

    public SetSecretNumberResponseDTO(String playerTurn, boolean secretNumberReady, String[] players) {
        this.playerTurn = playerTurn;
        this.secretNumberReady = secretNumberReady;
        this.players = players;
    }

    public String getPlayerTurn() {
        return playerTurn;
    }

    public boolean isSecretNumberReady() {
        return secretNumberReady;
    }

    public String[] getPlayers() {
        return players;
    }
}
