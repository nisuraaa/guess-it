package com.indezah.guess_it.services;

import com.indezah.guess_it.configs.SocketConnectionHandler;
import com.indezah.guess_it.dtos.CreateRoomDTO;
import com.indezah.guess_it.dtos.GuessResultDTO;
import com.indezah.guess_it.dtos.JoinRoomSuccessDTO;
import com.indezah.guess_it.dtos.SetSecretNumberResponseDTO;
import com.indezah.guess_it.exceptions.GameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private SocketConnectionHandler socketConnectionHandler;

    @InjectMocks
    private GameService gameService;

    // --- Helpers ---

    private CreateRoomDTO createRoom() {
        return gameService.create();
    }

    /**
     * Creates a room with both players joined and both secrets set.
     * Player 1 secret: "1234", Player 2 secret: "5678"
     * Returns [roomCode, player1Code, player2Code].
     */
    private String[] setupFullRoom() throws Exception {
        CreateRoomDTO room = gameService.create();
        JoinRoomSuccessDTO join = gameService.join(room.roomCode);
        gameService.setSecretNumber("1234", room.roomCode, room.playerCode);
        gameService.setSecretNumber("5678", room.roomCode, join.playerCode);
        return new String[]{room.roomCode, room.playerCode, join.playerCode};
    }

    // --- create() ---

    @Test
    void create_returnsValidSixDigitRoomCode() {
        CreateRoomDTO result = createRoom();
        assertThat(result.roomCode).matches("\\d{6}");
    }

    @Test
    void create_returnsNonBlankPlayerCode() {
        CreateRoomDTO result = createRoom();
        assertThat(result.playerCode).isNotBlank();
    }

    @Test
    void create_eachCallGeneratesUniqueRoom() {
        CreateRoomDTO first = createRoom();
        CreateRoomDTO second = createRoom();
        assertThat(first.playerCode).isNotEqualTo(second.playerCode);
    }

    // --- join() ---

    @Test
    void join_succeeds_andReturnsSameRoomCode() throws Exception {
        CreateRoomDTO room = createRoom();
        JoinRoomSuccessDTO result = gameService.join(room.roomCode);
        assertThat(result.roomCode).isEqualTo(room.roomCode);
    }

    @Test
    void join_assignsDistinctPlayerCode() throws Exception {
        CreateRoomDTO room = createRoom();
        JoinRoomSuccessDTO result = gameService.join(room.roomCode);
        assertThat(result.playerCode).isNotBlank();
        assertThat(result.playerCode).isNotEqualTo(room.playerCode);
    }

    @Test
    void join_throws_whenRoomNotFound() {
        assertThatThrownBy(() -> gameService.join("000000"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    void join_throws_whenRoomIsFull() throws Exception {
        CreateRoomDTO room = createRoom();
        gameService.join(room.roomCode);
        assertThatThrownBy(() -> gameService.join(room.roomCode))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("Room is full");
    }

    // --- setSecretNumber() ---

    @Test
    void setSecretNumber_player1Only_notReadyYet() throws Exception {
        CreateRoomDTO room = createRoom();
        gameService.join(room.roomCode);
        SetSecretNumberResponseDTO result = gameService.setSecretNumber("1234", room.roomCode, room.playerCode);
        assertThat(result.isSecretNumberReady()).isFalse();
        assertThat(result.getPlayerTurn()).isNull();
    }

    @Test
    void setSecretNumber_bothPlayers_returnsReady() throws Exception {
        CreateRoomDTO room = createRoom();
        JoinRoomSuccessDTO join = gameService.join(room.roomCode);
        gameService.setSecretNumber("1234", room.roomCode, room.playerCode);
        SetSecretNumberResponseDTO result = gameService.setSecretNumber("5678", room.roomCode, join.playerCode);
        assertThat(result.isSecretNumberReady()).isTrue();
    }

    @Test
    void setSecretNumber_bothPlayers_player1GoesFirst() throws Exception {
        CreateRoomDTO room = createRoom();
        JoinRoomSuccessDTO join = gameService.join(room.roomCode);
        gameService.setSecretNumber("1234", room.roomCode, room.playerCode);
        SetSecretNumberResponseDTO result = gameService.setSecretNumber("5678", room.roomCode, join.playerCode);
        assertThat(result.getPlayerTurn()).isEqualTo(room.playerCode);
    }

    @Test
    void setSecretNumber_bothPlayers_returnsAllPlayerCodes() throws Exception {
        CreateRoomDTO room = createRoom();
        JoinRoomSuccessDTO join = gameService.join(room.roomCode);
        gameService.setSecretNumber("1234", room.roomCode, room.playerCode);
        SetSecretNumberResponseDTO result = gameService.setSecretNumber("5678", room.roomCode, join.playerCode);
        assertThat(result.getPlayers()).containsExactlyInAnyOrder(room.playerCode, join.playerCode);
    }

    @Test
    void setSecretNumber_throws_whenTooShort() throws Exception {
        CreateRoomDTO room = createRoom();
        gameService.join(room.roomCode);
        assertThatThrownBy(() -> gameService.setSecretNumber("12", room.roomCode, room.playerCode))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("4 digits");
    }

    @Test
    void setSecretNumber_throws_whenContainsLetters() throws Exception {
        CreateRoomDTO room = createRoom();
        gameService.join(room.roomCode);
        assertThatThrownBy(() -> gameService.setSecretNumber("12ab", room.roomCode, room.playerCode))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("4 digits");
    }

    @Test
    void setSecretNumber_throws_whenNull() throws Exception {
        CreateRoomDTO room = createRoom();
        gameService.join(room.roomCode);
        assertThatThrownBy(() -> gameService.setSecretNumber(null, room.roomCode, room.playerCode))
                .isInstanceOf(GameException.class);
    }

    @Test
    void setSecretNumber_throws_whenPlayer1SetsTwice() throws Exception {
        CreateRoomDTO room = createRoom();
        gameService.join(room.roomCode);
        gameService.setSecretNumber("1234", room.roomCode, room.playerCode);
        assertThatThrownBy(() -> gameService.setSecretNumber("5678", room.roomCode, room.playerCode))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("already set");
    }

    @Test
    void setSecretNumber_throws_whenPlayer2SetsTwice() throws Exception {
        CreateRoomDTO room = createRoom();
        JoinRoomSuccessDTO join = gameService.join(room.roomCode);
        gameService.setSecretNumber("5678", room.roomCode, join.playerCode);
        assertThatThrownBy(() -> gameService.setSecretNumber("1234", room.roomCode, join.playerCode))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("already set");
    }

    @Test
    void setSecretNumber_throws_whenPlayerNotInRoom() throws Exception {
        CreateRoomDTO room = createRoom();
        assertThatThrownBy(() -> gameService.setSecretNumber("1234", room.roomCode, "unknown-player"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("Player not found");
    }

    // --- guessPlayer() ---

    @Test
    void guess_partialMatch_returnsCorrectPositions() throws Exception {
        // p2 secret = "5678"; guess "5178" → positions 0,2,3 match → [1,0,1,1]
        String[] s = setupFullRoom();
        GuessResultDTO result = gameService.guessPlayer(s[0], s[1], "5178");
        assertThat(result.correctPositions()).isEqualTo(new int[]{1, 0, 1, 1});
    }

    @Test
    void guess_noMatch_returnsAllZeros() throws Exception {
        // p2 secret = "5678"; guess "1234" → no position matches → [0,0,0,0]
        String[] s = setupFullRoom();
        GuessResultDTO result = gameService.guessPlayer(s[0], s[1], "1234");
        assertThat(result.correctPositions()).isEqualTo(new int[]{0, 0, 0, 0});
    }

    @Test
    void guess_allCorrect_setsActionToGameEnded() throws Exception {
        String[] s = setupFullRoom();
        GuessResultDTO result = gameService.guessPlayer(s[0], s[1], "5678");
        assertThat(result.action()).isEqualTo("GAME_ENDED");
    }

    @Test
    void guess_allCorrect_setsWinnerToGuessingPlayer() throws Exception {
        String[] s = setupFullRoom();
        GuessResultDTO result = gameService.guessPlayer(s[0], s[1], "5678");
        assertThat(result.winner()).isEqualTo(s[1]);
    }

    @Test
    void guess_notAllCorrect_setsActionToPostGuess() throws Exception {
        String[] s = setupFullRoom();
        GuessResultDTO result = gameService.guessPlayer(s[0], s[1], "5000");
        assertThat(result.action()).isEqualTo("POST_GUESS");
        assertThat(result.winner()).isNull();
    }

    @Test
    void guess_turnSwitchesToPlayer2_afterPlayer1Guesses() throws Exception {
        String[] s = setupFullRoom();
        GuessResultDTO result = gameService.guessPlayer(s[0], s[1], "5000");
        assertThat(result.playerTurn()).isEqualTo(s[2]);
    }

    @Test
    void guess_turnSwitchesToPlayer1_afterPlayer2Guesses() throws Exception {
        String[] s = setupFullRoom();
        gameService.guessPlayer(s[0], s[1], "5000");
        GuessResultDTO result = gameService.guessPlayer(s[0], s[2], "1000");
        assertThat(result.playerTurn()).isEqualTo(s[1]);
    }

    @Test
    void guess_player2Wins_whenCorrectlyGuessesPlayer1Secret() throws Exception {
        // p1 secret = "1234"
        String[] s = setupFullRoom();
        gameService.guessPlayer(s[0], s[1], "0000"); // player1 guesses wrong
        GuessResultDTO result = gameService.guessPlayer(s[0], s[2], "1234");
        assertThat(result.action()).isEqualTo("GAME_ENDED");
        assertThat(result.winner()).isEqualTo(s[2]);
    }

    @Test
    void guess_throws_whenNotYourTurn() throws Exception {
        String[] s = setupFullRoom(); // player1 goes first
        assertThatThrownBy(() -> gameService.guessPlayer(s[0], s[2], "1234"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("Not your turn");
    }

    @Test
    void guess_throws_whenPlayerNotInRoom() throws Exception {
        String[] s = setupFullRoom();
        assertThatThrownBy(() -> gameService.guessPlayer(s[0], "unknown-player", "1234"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("Players not found");
    }

    @Test
    void guess_throws_whenInvalidGuessFormat() throws Exception {
        String[] s = setupFullRoom();
        assertThatThrownBy(() -> gameService.guessPlayer(s[0], s[1], "12"))
                .isInstanceOf(GameException.class)
                .hasMessageContaining("4 digits");
    }

    @Test
    void guess_tracksHistoryForBothPlayers() throws Exception {
        String[] s = setupFullRoom();
        gameService.guessPlayer(s[0], s[1], "5100");
        gameService.guessPlayer(s[0], s[2], "1000");
        GuessResultDTO result = gameService.guessPlayer(s[0], s[1], "5200");
        assertThat(result.guessHistory().get(s[1])).hasSize(2);
        assertThat(result.guessHistory().get(s[2])).hasSize(1);
    }

    @Test
    void guess_playerCodeAndGuessedNumberReflectedInResult() throws Exception {
        String[] s = setupFullRoom();
        GuessResultDTO result = gameService.guessPlayer(s[0], s[1], "5600");
        assertThat(result.playerCode()).isEqualTo(s[1]);
        assertThat(result.guessedNumber()).isEqualTo("5600");
    }
}
