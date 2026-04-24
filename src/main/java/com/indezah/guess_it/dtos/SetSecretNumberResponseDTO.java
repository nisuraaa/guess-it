package com.indezah.guess_it.dtos;

import java.util.Map;

public record SetSecretNumberResponseDTO(
        String playerTurn,
        boolean secretNumberReady,
        String[] players,
        Map<String, String> playerNames
) {}
