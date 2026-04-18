package com.indezah.guess_it.models;

import java.time.LocalDateTime;

public record Guess(
    String playerCode,    
    String guessedNumber, 
    int[] correctPositions,
    LocalDateTime timestamp
) {
    public Guess {
        timestamp = LocalDateTime.now();
    }
}