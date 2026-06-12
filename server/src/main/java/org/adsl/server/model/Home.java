package org.adsl.server.model;


import org.adsl.server.network.HomeObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Server-side "home screen" model that tracks the list of currently open games
 * and notifies connected {@link HomeObserver}s (i.e. clients in the lobby)
 * whenever the game list changes.
 */
public class Home {
    private final List<Integer> games;
    private final List<HomeObserver> observers;

    /** Creates an empty home model with no open games and no observers. */
    public Home() {
        this.observers = new ArrayList<>();
        this.games = new ArrayList<>();
    }

    /**
     * Registers a newly created game so it appears in the lobby list.
     *
     * @param gameId identifier of the new game
     */
    public void addGame(int gameId){
        games.add(gameId);
    }

    /**
     * Removes a game from the lobby list, e.g. once it has started or ended.
     *
     * @param gameId identifier of the game to remove
     */
    public void removeGame(Integer gameId){
        games.remove(gameId);
    }

    /** Notifies all observers with a snapshot of the current open-game list. */
    public void update() {
        List<Integer> snapshot = new ArrayList<>(games);
        for(HomeObserver o : observers) o.updateHome(snapshot);
    }

    /**
     * Notifies all observers with the current open-game list and a log message.
     *
     * @param message log message to accompany the update
     */
    public void update(String message) {
        List<Integer> snapshot = new ArrayList<>(games);
        for(HomeObserver o : observers) o.updateHome(snapshot, message);
    }

    /**
     * Notifies all observers with the current open-game list along with each
     * game's player roster and capacity.
     *
     * @param gamePlayers  for each game id, the names of players currently in its lobby
     * @param gameCapacity for each game id, the maximum number of players
     */
    public void update(Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity) {
        List<Integer> snapshot = new ArrayList<>(games);
        for (HomeObserver o : observers) o.updateHome(snapshot, gamePlayers, gameCapacity);
    }

    /**
     * Notifies all observers with the current open-game list, each game's player
     * roster and capacity, and a log message.
     *
     * @param gamePlayers  for each game id, the names of players currently in its lobby
     * @param gameCapacity for each game id, the maximum number of players
     * @param message      log message to accompany the update
     */
    public void update(Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity, String message) {
        List<Integer> snapshot = new ArrayList<>(games);
        for (HomeObserver o : observers) o.updateHome(snapshot, gamePlayers, gameCapacity, message);
    }

    /**
     * Registers an observer to receive lobby updates.
     *
     * @param observer the observer to add
     */
    public void addObserver(HomeObserver observer) {
        this.observers.add(observer);
    }

    /**
     * Unregisters a previously added observer.
     *
     * @param observer the observer to remove
     */
    public void removeObserver(HomeObserver observer) {
        this.observers.remove(observer);
    }
}
