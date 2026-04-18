package com.indezah.guess_it.controllers;

import com.indezah.guess_it.dtos.CreateRoomDTO;
import com.indezah.guess_it.dtos.JoinRoomDTO;
import com.indezah.guess_it.services.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = { "https://guess-it-fe-production.up.railway.app"})
@RequestMapping("/api")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/room")
    public ResponseEntity<?> createRoom() {
        CreateRoomDTO code = gameService.create();
        return ResponseEntity.ok().body(code);
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinRoom(@RequestBody JoinRoomDTO joinRoomDTO) throws Exception {
        return ResponseEntity.ok().body(gameService.join(joinRoomDTO.roomCode));
    }
}
