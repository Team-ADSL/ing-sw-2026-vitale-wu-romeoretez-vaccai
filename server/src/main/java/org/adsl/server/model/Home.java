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

    public Home() {
        this.observers = new ArrayList<>();
        this.games = new ArrayList<>();
    }

    public void addGame(int gameId){
        games.add(gameId);
    }

    public void removeGame(Integer gameId){
        games.remove(gameId);
    }

    public void update() {
        List<Integer> snapshot = new ArrayList<>(games);
        for(HomeObserver o : observers) o.updateHome(snapshot);
    }
    public void update(String message) {
        List<Integer> snapshot = new ArrayList<>(games);
        for(HomeObserver o : observers) o.updateHome(snapshot, message);
    }

    public void update(Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity) {
        List<Integer> snapshot = new ArrayList<>(games);
        for (HomeObserver o : observers) o.updateHome(snapshot, gamePlayers, gameCapacity);
    }

    public void update(Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity, String message) {
        List<Integer> snapshot = new ArrayList<>(games);
        for (HomeObserver o : observers) o.updateHome(snapshot, gamePlayers, gameCapacity, message);
    }
    public void addObserver(HomeObserver observer) {
        this.observers.add(observer);
    }
    public void removeObserver(HomeObserver observer) {
        this.observers.remove(observer);
    }
}
