package org.example.model.game;

import java.util.Set;

public class Game {
    private int round;
    private int era;
    private Set<Player> players;
    private Board board;

    public Game (int round, int era, Set<Player> players, Board board) {
        this.round = round;
        this.era = era;
        this.players = players;
        this.board = board;
    }

    public Board getBoard(){
        return this.board;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public int getEra() {
        return era;
    }

    public int getRound() {
        return round;
    }
}
