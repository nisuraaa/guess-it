package com.indezah.guess_it.services;

import com.indezah.guess_it.configs.SocketConnectionHandler;
import com.indezah.guess_it.dtos.CreateRoomDTO;
import com.indezah.guess_it.dtos.GuessResultDTO;
import com.indezah.guess_it.dtos.JoinRoomSuccessDTO;
import com.indezah.guess_it.dtos.SetSecretNumberResponseDTO;
import com.indezah.guess_it.exceptions.GameException;
import com.indezah.guess_it.models.Guess;
import com.indezah.guess_it.models.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameService {

    private final ConcurrentHashMap<String, Room> roomStore = new ConcurrentHashMap<>();

    @Autowired
    @Lazy
    private SocketConnectionHandler socketConnectionHandler;

    public CreateRoomDTO create() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000);
        Room room = new Room();
        room.setRoomCode(String.valueOf(code));
        UUID uuid = UUID.randomUUID(); // Create p1 userID
        room.setPlayer1(uuid.toString());
        roomStore.put(room.getRoomCode(), room);
        return new CreateRoomDTO(room.getRoomCode(), room.getPlayer1());
    }

    private Room requireRoom(String roomCode) {
        Room room = roomStore.get(roomCode);
        if (room == null) {
            throw new GameException("Room not found: " + roomCode);
        }
        return room;
    }

    public JoinRoomSuccessDTO join(String roomCode) throws Exception {
        Room room = requireRoom(roomCode);
        if (room.getPlayer2() != null) {
            throw new GameException("Room is full");
        }
        room.setPlayer2(UUID.randomUUID().toString());
        return new JoinRoomSuccessDTO(room.getRoomCode(), room.getPlayer2());
    }

    public SetSecretNumberResponseDTO setSecretNumber(String secretNumber, String roomCode, String playerCode)
            throws Exception {
        validateSecretNumber(secretNumber);
        Room room = requireRoom(roomCode);
        if (room.getPlayer1().equals(playerCode)) {
            if (room.getPlayer1Secret() != null) {
                throw new GameException("Player 1 already set their secret number");
            }
            room.setPlayer1Secret(secretNumber);
        } else if (room.getPlayer2() != null && room.getPlayer2().equals(playerCode)) {
            if (room.getPlayer2Secret() != null) {
                throw new GameException("Player 2 already set their secret number");
            }
            room.setPlayer2Secret(secretNumber);
        } else {
            throw new GameException("Player not found in room");
        }

        boolean bothReady = room.getPlayer1Secret() != null && room.getPlayer2Secret() != null;

        if (bothReady) {
            room.setPlayerTurn(1);
            String firstPlayer = room.getPlayer1();
            return new SetSecretNumberResponseDTO(firstPlayer, true,
                    new String[] { room.getPlayer1(), room.getPlayer2() });
        }
        return new SetSecretNumberResponseDTO(null, false, null);
    }

    private void validateSecretNumber(String secretNumber) {
        if (secretNumber == null || !secretNumber.matches("\\d{4}")) {
            throw new GameException("Secret must be exactly 4 digits");
        }
    }

    private int[] evaluateGuess(String secretNumber, String guessedNumber) {
        int[] correctPositions = new int[4];
        for (int i = 0; i < secretNumber.length(); i++) {
            if (secretNumber.charAt(i) == guessedNumber.charAt(i)) {
                correctPositions[i] = 1;
            }
        }
        return correctPositions;
    }

    private void updatePlayerGuesses(int[] tracking, int[] correctPositions) {
        for (int i = 0; i < correctPositions.length; i++) {
            if (correctPositions[i] == 1) {
                // Update the appropriate player's guess array
                tracking[i] = 1;
            }
        }
    }

    public GuessResultDTO guessPlayer(String roomCode, String playerCode, String guessedNumber) {
        try {
            validateSecretNumber(guessedNumber);
        } catch (GameException e) {
            throw e;
        }

        Room room = requireRoom(roomCode);

        boolean isPlayer1 = playerCode.equals(room.getPlayer1());
        boolean isPlayer2 = playerCode.equals(room.getPlayer2()) && room.getPlayer2() != null;

        if (!isPlayer1 && !isPlayer2) {
            throw new GameException("Players not found");
        }

        int expectedTurn = isPlayer1 ? 1 : 2;

        if (room.getPlayerTurn() != expectedTurn) {
            throw new GameException("Not your turn");
        }

        int[] correctPositions = evaluateGuess(isPlayer1 ? room.getPlayer2Secret() : room.getPlayer1Secret(),
                guessedNumber);

        Guess guess = new Guess(playerCode, guessedNumber, correctPositions, null);
        if (isPlayer1) {
            room.addPlayer1Guess(guess);
            updatePlayerGuesses(room.getPlayer1SecretGuesses(), correctPositions);
            room.setPlayerTurn(2);
        } else {
            room.addPlayer2Guess(guess);
            updatePlayerGuesses(room.getPlayer2SecretGuesses(), correctPositions);
            room.setPlayerTurn(1);
        }

        boolean gameEnded = Arrays.stream(correctPositions).allMatch(x -> x == 1);
        if (!gameEnded) {
            gameEnded = Arrays.stream(correctPositions).allMatch(x -> x == 1);
        }

        Map<String, List<Guess>> guessHistory = new HashMap<>();
        guessHistory.put(room.getPlayer1(), room.getPlayer1GuessHistory());
        if (room.getPlayer2() != null) {
            guessHistory.put(room.getPlayer2(), room.getPlayer2GuessHistory());
        }

        return new GuessResultDTO(
                gameEnded ? "GAME_ENDED" : "POST_GUESS",
                gameEnded ? playerCode : null,
                correctPositions,
                playerCode,
                guessedNumber,
                guessHistory,
                gameEnded ? null : (room.getPlayerTurn() == 1 ? room.getPlayer1() : room.getPlayer2()));
    }

}
