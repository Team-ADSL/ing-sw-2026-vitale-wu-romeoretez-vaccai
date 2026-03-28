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
    public void updateAll(String error) {
        for(ModelObserver o : observers) o.update(createDto(error));
    }

    @Override
    public void addObserver(ModelObserver observer) {
        this.observers.add(observer);
    }

    @Override
    public void removeObserver(ModelObserver observer) {
        this.observers.remove(observer);
    }

    public LobbyDTO createDto(String error){
        return null;
    }

    public List<Integer> getGames() {
        return games;
    }


}
