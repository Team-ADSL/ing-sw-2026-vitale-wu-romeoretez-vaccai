package org.example.server.model;

import org.example.server.model.board.Board;
import org.example.shared.enums.Phase;
import org.example.shared.model.GameDTO;

import java.util.*;

public class Game implements Datasource {
    private int round;
    private int era;
    private Set<Player> players;
    private Optional<Player> currentPlayer;
    private Board board;
    private Phase phase;
    private final List<ModelObserver> observers = new ArrayList<>();
    private boolean isInitialized;


    public Game() {
        this.round = 0;
        this.era = 1;
        this.players = new HashSet<>();
        this.currentPlayer = Optional.empty();
        this.board = null; // TO modify
        this.phase = Phase.LOBBY;
        this.isInitialized = false;
    }

    public Game(int round, int era, Set<Player> players, Optional<Player> currentPlayer, Board board, Phase phase) {
        this.round = round;
        this.era = era;
        this.players = players;
        this.currentPlayer = currentPlayer;
        this.board = board;
        this.phase = phase;
        this.isInitialized = true;
    }

    @Override
    public void addObserver(ModelObserver o) {
        observers.add(o);
    }
    @Override
    public void removeObserver(ModelObserver o) {
        observers.remove(o);
    }
    @Override
    public void updateAll(){
        for(ModelObserver o : observers) o.update(createDTO());
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

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setCurrentPlayer(Optional<Player> currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
    }
}
