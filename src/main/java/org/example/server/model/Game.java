package org.example.server.model;

import org.example.server.model.board.Board;
import org.example.shared.enums.Phase;
import org.example.shared.model.GameDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class Game {
    private int round;
    private int era;
    private Set<Player> players;
    private Optional<Player> currentPlayer; // Usefull for extraMove action
    private Board board;
    private Phase phase;
    private final List<ModelObserver> observers = new ArrayList<>();

    public Game (int round, int era, Set<Player> players, Optional<Player> currentPlayer, Board board) {
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
    public GameDTO createDTO() {
        return null;
    }

    public void changeEra() {
        this.era = era + 1;
    }

    public Board getBoard(){
        return this.board;
    }
    public Set<Player> getPlayers() {
        return players;
    }
    public Optional<Player> getCurrentPlayer() {
        return currentPlayer;
    }
    public int getEra() {
        return era;
    }
    public int getRound() {
        return round;
    }
    public Phase getPhase() {
        return phase;
    }
}
