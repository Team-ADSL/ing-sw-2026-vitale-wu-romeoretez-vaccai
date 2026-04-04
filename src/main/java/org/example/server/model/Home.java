package org.example.server.model;


import java.util.ArrayList;
import java.util.List;

public class Home {
    private final List<Integer> games;
    private final List<ModelObserver> observers;

    public Home() {
        this.observers = new ArrayList<>();
        this.games = new ArrayList<>();
    }

    public void update() {
        for(ModelObserver o : observers) o.updateHome(games);
    }
    public void addObserver(ModelObserver observer) {
        this.observers.add(observer);
    }
    public void removeObserver(ModelObserver observer) {
        this.observers.remove(observer);
    }
}
