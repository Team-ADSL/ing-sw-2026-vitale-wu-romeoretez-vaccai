package org.example.model.game;

import java.util.Optional;
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

    public void checkBuildings(Optional<Player> player){

    }

    public void declareWinner(){

    }

    public Board getBoard(){
        return this.board;
    }

    public Set<Player> getPlayers() {
        return players;
    }
}
