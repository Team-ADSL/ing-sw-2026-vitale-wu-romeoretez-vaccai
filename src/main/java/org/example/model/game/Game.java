package org.example.model.game;

import java.util.Set;

public class Game {

    private int round;
    private int era;
    private Set<Player> players;
    private Board board;

    public Game (int round, int era, Set<Player> players) {
        this.round = round;
        this.era = era;
        this.players = players;
    }

}
