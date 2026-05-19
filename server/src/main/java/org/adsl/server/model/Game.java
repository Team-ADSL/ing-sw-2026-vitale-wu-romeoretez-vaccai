package org.adsl.server.model;

import org.adsl.server.network.VirtualClient;
import org.adsl.server.model.board.Board;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.*;


import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core game model. Holds all mutable state for a single match: players, board,
 * current round/era, current player, and phase.
 * <p>
 * Notifies registered {@link GameObserver}s on every state change and
 * {@link EndGameObserver}s when the game ends. Observer lists are transient
 * and rebuilt after deserialisation via {@link #setupTransientAttributes()}.
 * </p>
 * <p>
 * Two constructors are provided: one for new games, one for games recovered
 * from serialised state (the recovered constructor sets {@code isInitialized}
 * to {@code true} to skip board setup in {@code InitGameState}).
 * </p>
 */
public class Game implements Serializable {
    private final int gameId;
    private final Set<Player> players;
    private final int numPlayer;
    private Board board;
    private int round;
    private int era;
    private Player currentPlayer;
    private Phase phase;

    private transient boolean isInitialized;
    private transient List<GameObserver> gameObservers;
    private transient List<EndGameObserver> endGameObservers;

    // For initial istantiation
    public Game(int gameId, int numPlayer) {
        this.gameId = gameId;
        this.numPlayer = numPlayer;
        this.players = new HashSet<>();
        this.board = null;
        this.round = 1;
        this.era = 1;
        this.currentPlayer = null;
        this.phase = null;
        this.isInitialized = false;
        gameObservers = new ArrayList<>();
        endGameObservers = new ArrayList<>();
    }

    // For recover after crash
    public Game(int gameId, int numPlayer, int round, int era, Set<Player> players, Player currentPlayer,
                Board board, Phase phase) {
        this.gameId = gameId;
        this.numPlayer = numPlayer;
        this.players = players;
        this.board = board;
        this.round = round;
        this.era = era;
        this.currentPlayer = currentPlayer;
        this.phase = phase;
        this.isInitialized = true;
        gameObservers = new ArrayList<>();
        endGameObservers = new ArrayList<>();
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
        Set<PlayerDTO> playersDTO = players.stream().map(Player::createDTO).collect(Collectors.toSet());
        BoardDTO boardDTO = board.createDTO();
        Totem currentTotem = (currentPlayer != null) ? currentPlayer.getColor() : null;
        return new GameDTO(gameId, numPlayer, round, era, playersDTO, boardDTO, phase, currentTotem);
    }

    public void addObserver(EndGameObserver o) {
        endGameObservers.add(o);
    }
    public void removeObserver(EndGameObserver o) {
        endGameObservers.remove(o);
    }

    public void sendUpdateLobby(){
        List<String> playerNames = players.stream().filter(Player::isActive).map(Player::getName).toList();
        for(GameObserver o : gameObservers) o.updateLobby(gameId, playerNames, numPlayer);
    }
    public void sendUpdateLobby(String message){
        List<String> playerNames = players.stream().filter(Player::isActive).map(Player::getName).toList();
        for(GameObserver o : gameObservers) o.updateLobby(gameId, playerNames, numPlayer, message);
    }
    public void sendTotemAvailable(List<Totem> totemAvailable, String message){
        for(GameObserver o : gameObservers) o.updateTotemAvailable(totemAvailable, message);
    }
    public void sendUpdateGame(){
        for(GameObserver o : gameObservers) o.updateGame(this);
    }
    public void sendUpdateGame(String message){
        for(GameObserver o : gameObservers) o.updateGame(this, message);
    }
    public void broadcastError(String message){
        for(GameObserver o : gameObservers) o.notifyError(message);
    }
    public void sendEventTriggered(String eventTitle, String logMessage){
        for(GameObserver o : gameObservers) o.notifyEventTriggered(eventTitle, logMessage);
    }
    public void sendEndGameResults(List<MatchResult> results, List<DBRecord> records){
        for(EndGameObserver o : endGameObservers) o.notifyEndGame(gameId, results, records);
    }
    public void sendEndGameResults(List<MatchResult> results, List<DBRecord> records, String message){
        for(EndGameObserver o : endGameObservers) o.notifyEndGame(gameId, results, records, message);
    }

    /**
     * Reinitialises transient fields after deserialisation. Must be called on
     * every recovered {@link Game} before it is registered with a controller.
     */
    public void setupTransientAttributes(){
        isInitialized = true;
        gameObservers = new ArrayList<>();
        endGameObservers = new ArrayList<>();
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
    public int getNumPlayer() {
        return numPlayer;
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
