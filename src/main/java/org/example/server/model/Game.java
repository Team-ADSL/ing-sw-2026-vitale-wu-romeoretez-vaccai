package org.example.server.model;

import org.example.server.network.VirtualClient;
import org.example.server.model.board.Board;
import org.example.shared.enums.Phase;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;


import java.util.*;

public class Game {
    private final int gameId;
    private final List<GameObserver> gameObservers = new ArrayList<>();
    private final List<EndGameObserver> endGameObservers = new ArrayList<>();
    private final Set<Player> players;

    private Board board;
    private int round;
    private int era;
    private Player currentPlayer;
    private Phase phase;
    private boolean isInitialized;

    // For initial istantiation
    public Game(int gameId, EndGameObserver endGameObserver) {
        this.gameId = gameId;
        addObserver(endGameObserver);
        this.players = new HashSet<>();

        this.board = null;
        this.round = 0;
        this.era = 1;
        this.currentPlayer = null;
        this.phase = Phase.LOBBY;
        this.isInitialized = false;
    }

    // For recover after crash
    public Game(int gameId, int round, int era, Set<Player> players, Player currentPlayer,
                Board board, Phase phase, boolean isInitialized,
                EndGameObserver endGameObserver, GameObserver gamePersistenceManager) {
        this.gameId = gameId;
        addObserver(endGameObserver);
        addObserver(gamePersistenceManager);

        this.round = round;
        this.era = era;
        this.players = players;
        this.currentPlayer = currentPlayer;
        this.board = board;
        this.phase = phase;
        this.isInitialized = isInitialized;
    }

    public void addVirtualClient(VirtualClient virtualClient){
        addObserver((GameObserver) virtualClient);
        addObserver((EndGameObserver) virtualClient);
    }
    public void removeVirtualClient(VirtualClient virtualClient){
        removeObserver((GameObserver) virtualClient);
        removeObserver((EndGameObserver) virtualClient);
    }

    public void addObserver(GameObserver o) {
        gameObservers.add(o);
    }
    public void removeObserver(GameObserver o) {
        gameObservers.remove(o);
    }
    public GameDTO createDTO() {
        return null; // TO IMPLEMENT
    }

    public void addObserver(EndGameObserver o) {
        endGameObservers.add(o);
    }
    public void removeObserver(EndGameObserver o) {
        endGameObservers.remove(o);
    }

    public void sendUpdateLobby(){
        List<String> playerNames = players.stream().map(Player::getName).toList();
        for(GameObserver o : gameObservers) o.updateLobby(playerNames);
    }
    public void sendUpdateGame(){
        for(GameObserver o : gameObservers) o.updateGame(createDTO());
    }
    public void sendEndGameResults(List<MatchResult> results){
        for(EndGameObserver o : endGameObservers) o.notifyEndGame(gameId, results);
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
        return Optional.ofNullable(currentPlayer);
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

    public void setCurrentPlayer(Player currentPlayer) {
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
