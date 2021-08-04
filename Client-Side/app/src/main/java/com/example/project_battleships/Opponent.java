package com.example.project_battleships;

import java.io.Serializable;

public class Opponent implements Serializable {
    private CharactersBoard board;
    private int turns;

    public Opponent() {
        this.board = new CharactersBoard();
        this.turns = 0;
    }

    public CharactersBoard getBoard() { return this.board; }

    public void setBoard(CharactersBoard board) { this.board = board; }

    public int getTurns() { return this.turns; }

    public void incTurns() { this.turns++; }
}
