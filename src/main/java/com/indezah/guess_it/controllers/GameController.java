package com.indezah.guess_it.controllers;

import com.indezah.guess_it.dtos.CreateRoomDTO;
import com.indezah.guess_it.dtos.CreateRoomRequestDTO;
import com.indezah.guess_it.dtos.JoinRoomDTO;
import com.indezah.guess_it.dtos.JoinRoomSuccessDTO;
import com.indezah.guess_it.services.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = { "https://guess-it-fe-production.up.railway.app", "http://localhost:5173" })
@RequestMapping("/api")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/room")
    public ResponseEntity<CreateRoomDTO> createRoom(@RequestBody CreateRoomRequestDTO dto) {
        return ResponseEntity.ok(gameService.create(dto.playerName()));
    }

    @PostMapping("/join")
    public ResponseEntity<JoinRoomSuccessDTO> joinRoom(@RequestBody JoinRoomDTO joinRoomDTO) throws Exception {
        return ResponseEntity.ok(gameService.join(joinRoomDTO.roomCode(), joinRoomDTO.playerName()));
    }
}
