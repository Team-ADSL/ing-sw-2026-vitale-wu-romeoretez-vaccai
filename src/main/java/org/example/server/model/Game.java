package org.example.server.model;

import org.example.server.network.VirtualClient;
import org.example.server.model.board.Board;
import org.example.shared.enums.Phase;
import org.example.shared.model.MatchResult;

import java.sql.SQLException;
import java.util.*;

public class Game implements Datasource, EndGameData {
    private final int gameId;

    private int round;
    private int era;
    private final Set<Player> players;
    private Optional<Player> currentPlayer;
    private Board board;
    private Phase phase;
    private boolean isInitialized;

    private final List<ModelObserver> modelObservers = new ArrayList<>();
    private final List<EndGameObserver> endGameObservers = new ArrayList<>();

    // For initial istantiation
    public Game(int gameId, EndGameObserver endGameObserver, ModelObserver gamePersistenceManager) {
        this.gameId = gameId;
        this.round = 0;
        this.era = 1;
        this.players = new HashSet<>();
        this.currentPlayer = Optional.empty();
        this.board = null;
        this.phase = Phase.LOBBY;
        this.isInitialized = false;
        addObserver(endGameObserver);
        addObserver(gamePersistenceManager);
    }

    // For recover after crash
    public Game(int gameId, int round, int era, Set<Player> players, Optional<Player> currentPlayer,
                Board board, Phase phase, boolean isInitialized,
                EndGameObserver endGameObserver, ModelObserver gamePersistenceManager) {
        this.gameId = gameId;
        this.round = round;
        this.era = era;
        this.players = players;
        this.currentPlayer = currentPlayer;
        this.board = board;
        this.phase = phase;
        this.isInitialized = isInitialized;
        addObserver(endGameObserver);
        addObserver(gamePersistenceManager);
    }

    public void addVirtualClient(VirtualClient virtualClient){
        addObserver((ModelObserver) virtualClient);
        addObserver((EndGameObserver) virtualClient);
    }

    public void removeVirtualClient(VirtualClient virtualClient){
        removeObserver((ModelObserver) virtualClient);
        removeObserver((EndGameObserver) virtualClient);
    }

    @Override
    public void addObserver(ModelObserver o) {
        modelObservers.add(o);
    }

    @Override
    public void removeObserver(ModelObserver o) {
        modelObservers.remove(o);
    }

    @Override
    public void updateAll(){
        for(ModelObserver o : modelObservers) o.update(this);
    }

    @Override
    public void addObserver(EndGameObserver o) {
        endGameObservers.add(o);
    }

    @Override
    public void removeObserver(EndGameObserver o) {
        endGameObservers.remove(o);
    }

    @Override
    public void notifyEndGame(List<MatchResult> matchResults) {
        for(EndGameObserver o : endGameObservers) o.update(gameId, matchResults);
    }

    public void changeEra() {
        this.era += 1;
    }
    public void changeRound(){this.round += 1; }

    public int getGameId() {
        return gameId;
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
    public void setBoard(Board board) {
        this.board = board;
    }
    public void setInitialized(boolean initialized) {
        isInitialized = initialized;
    }
}
