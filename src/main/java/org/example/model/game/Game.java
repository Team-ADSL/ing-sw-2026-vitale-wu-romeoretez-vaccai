package org.example.model.game;

import org.example.view.ModelObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Game {
    private int round;
    private int era;
    private Set<Player> players;
    private Player currentPlayer; // Usefull for extraMove actions
    private Board board;
    private final List<ModelObserver> observers = new ArrayList<>();

    public Game (int round, int era, Set<Player> players, Player currentPlayer, Board board) {
        this.round = round;
        this.era = era;
        this.players = players;
        this.currentPlayer = currentPlayer;
        this.board = board;
    }


    public void addObserver(ModelObserver o) {
        observers.add(o);
    }

    public void removeListener(ModelObserver o) {
        observers.remove(o);
    }

    public void updateAll(String error){
        for(ModelObserver o : observers) o.update(createDTO(), error);
    }

    public Game createDTO(){
        return this; // Create the "smaller" model
    }

    public Board getBoard(){
        return this.board;
    }

    public Set<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public int getEra() {
        return era;
    }

    public int getRound() {
        return round;
    }

    public void changeEra() {
        this.era = era + 1;
    }
}
