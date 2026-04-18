package com.indezah.guess_it.dtos;


public class CreateRoomDTO
{
    public String roomCode;
    public String playerCode;

    public CreateRoomDTO(String roomCode, String playerCode) {
        this.roomCode = roomCode;
        this.playerCode = playerCode;
    }
}
