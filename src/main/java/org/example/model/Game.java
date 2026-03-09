package org.example.model;

import java.util.ArrayList;
import java.util.Set;

public class Game {

    private int round;
    private int turn;
    private int era;
    private Phase phase;
    private Set<Player> players;

    public Game (int round, int turn, int era, Phase phase, Set<Player> players) {

        this.round = round;
        this.turn = turn;
        this.era = era;
        this.phase = phase;
        this.players = players;

    }

}
