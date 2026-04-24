package com.indezah.guess_it.configs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.indezah.guess_it.dtos.WebSocketMessageDTO;
import com.indezah.guess_it.exceptions.GameException;
import com.indezah.guess_it.dtos.GuessResultDTO;
import com.indezah.guess_it.dtos.SetSecretNumberResponseDTO;
import com.indezah.guess_it.services.GameService;

@Component
public class SocketConnectionHandler extends TextWebSocketHandler {

    @Autowired
    private GameService gameService;

    @Autowired
    private ObjectMapper objectMapper;

    List<WebSocketSession> webSocketSessions = Collections.synchronizedList(new ArrayList<>());

    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private static final long RATE_LIMIT_MS = 100; // Max 10 requests per second

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        super.afterConnectionEstablished(session);

        // Adding the session into the list
        webSocketSessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        // Removing the connection info from the list
        webSocketSessions.remove(session);
        lastRequestTime.remove(session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        super.handleMessage(session, message);

        // Rate limiting check
        String sessionId = session.getId();
        Long lastRequest = lastRequestTime.get(sessionId);
        long now = System.currentTimeMillis();
        if (lastRequest != null && (now - lastRequest) < RATE_LIMIT_MS) {
            String response = objectMapper
                    .writeValueAsString(Map.of("success", false, "message", "Rate limit exceeded", "action", "ERROR"));
            session.sendMessage(new TextMessage(response));
            return;
        }
        lastRequestTime.put(sessionId, now);

        WebSocketMessageDTO dto = objectMapper.readValue(message.getPayload().toString(), WebSocketMessageDTO.class);

        if ("SET_SECRET".equals(dto.action)) {
            try {
                SetSecretNumberResponseDTO result = gameService.setSecretNumber(dto.secretNumber, dto.roomCode,
                        dto.playerCode);

                if (result.secretNumberReady()) {
                    String response = objectMapper.writeValueAsString(Map.of("success", result.secretNumberReady(),
                            "message", "Secret number set successfully", "action", "SECRET_SET", "playerTurn",
                            result.playerTurn(), "players", result.players(), "playerNames", result.playerNames()));
                    synchronized (webSocketSessions) {
                        for (WebSocketSession s : webSocketSessions) {
                            if (s.isOpen()) {
                                s.sendMessage(new TextMessage(response));
                            }
                        }
                    }
                } else {
                    String response = objectMapper.writeValueAsString(Map.of("success", result.secretNumberReady(),
                            "message", "Secret number not set", "action", "SECRET_SET"));
                    session.sendMessage(new TextMessage(response));
                }
            } catch (GameException e) {
                String response = objectMapper.writeValueAsString(Map.of("success", false, "message", e.getMessage(),
                        "action", "SECRET_SET"));
                session.sendMessage(new TextMessage(response));
            }
        }
        if ("GUESS".equals(dto.action)) {
            GuessResultDTO result;
            try {
                result = gameService.guessPlayer(dto.roomCode, dto.playerCode, dto.guessedNumber);
            } catch (GameException e) {
                String response = objectMapper
                        .writeValueAsString(Map.of("success", false, "message", e.getMessage(), "action", "GUESS"));
                session.sendMessage(new TextMessage(response));
                return;
            }
            String response = objectMapper.writeValueAsString(result);
            synchronized (webSocketSessions) {
                for (WebSocketSession s : webSocketSessions) {
                    if (s.isOpen()) {
                        s.sendMessage(new TextMessage(response));
                    }
                }
            }

        }
    }
}