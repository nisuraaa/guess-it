package com.indezah.guess_it.dtos;

import java.util.List;
import java.util.Map;

import com.indezah.guess_it.models.Guess;

public record GuessResultDTO(
    String action,
    String winner,
    int[] correctPositions,
    String playerCode,
    String guessedNumber,
    Map<String, List<Guess>> guessHistory,
    String playerTurn
) {}