package com.indezah.guess_it.dtos;

public class Result {
    public String[] correctPositions;
    
    public Result(String[] correctPositions) {
        this.correctPositions = correctPositions;
    }
    
    public String[] getCorrectPositions() {
        return correctPositions;
    }
    
    public void setCorrectPositions(String[] correctPositions) {
        this.correctPositions = correctPositions;
    }
} 