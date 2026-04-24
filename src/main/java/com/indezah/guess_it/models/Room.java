package com.indezah.guess_it.models;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class Room {

    private String roomCode;
    private String player1;
    private String player2;
    private String player1Name;
    private String player2Name;
    private String player1Secret;
    private String player2Secret;
    private int[] player1SecretGuesses = { 0, 0, 0, 0 };
    private int[] player2SecretGuesses = { 0, 0, 0, 0 };
    private int playerTurn;
    private List<Guess> player1GuessHistory = new ArrayList<>();
    private List<Guess> player2GuessHistory = new ArrayList<>();

    public void addPlayer1Guess(Guess guess) {
        this.player1GuessHistory.add(guess);
    }

    public void addPlayer2Guess(Guess guess) {
        this.player2GuessHistory.add(guess);
    }

}
