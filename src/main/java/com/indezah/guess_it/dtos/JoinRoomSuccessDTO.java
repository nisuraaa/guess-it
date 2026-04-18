package com.indezah.guess_it.dtos;

public class JoinRoomSuccessDTO {
    public String roomCode;
    public String playerCode;


    public JoinRoomSuccessDTO(String roomCode, String playerCode) {
        this.roomCode = roomCode;
        this.playerCode = playerCode;
    }
}
