package org.example.server.model;

import org.example.shared.model.LobbyDTO;

import java.util.ArrayList;
import java.util.List;

public class Lobby implements Datasource {
    private final List<Integer> games;
    private final List<ModelObserver> observers;

    public Lobby() {
        this.observers = new ArrayList<>();
        this.games = new ArrayList<>();
    }

    @Override
    public void updateAll() {
        for(ModelObserver o : observers) o.update(createDto());
    }

    @Override
    public void addObserver(ModelObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void removeObserver(ModelObserver observer) {
        this.observers.remove(observer);
    }

    public LobbyDTO createDto(){
        return null;
    }

    public List<Integer> getGames() {
        return games;
    }


}
