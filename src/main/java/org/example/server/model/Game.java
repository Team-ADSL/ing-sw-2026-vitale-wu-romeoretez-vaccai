package org.example.server.model;

import org.example.server.network.VirtualClient;
import org.example.server.model.board.Board;
import org.example.shared.enums.Phase;
import org.example.shared.model.BoardDTO;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.example.shared.model.PlayerDTO;


import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private final int gameId;
    private final Set<Player> players;
    private final int numPlayer;
    private Board board;
    private int round;
    private int era;
    private Player currentPlayer;
    private Phase phase;

    private transient boolean isInitialized;
    private final transient List<GameObserver> gameObservers = new ArrayList<>();
    private final transient List<EndGameObserver> endGameObservers = new ArrayList<>();

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
        return new GameDTO(gameId, numPlayer, round, era, playersDTO, boardDTO, phase);
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
