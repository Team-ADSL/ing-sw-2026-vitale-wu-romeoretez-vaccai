package org.adsl.server.model;


import org.adsl.server.network.HomeObserver;

import java.util.ArrayList;
import java.util.List;

public class Home {
    private final List<Integer> games;
    private final List<HomeObserver> observers;

    public Home() {
        this.observers = new ArrayList<>();
        this.games = new ArrayList<>();
    }

    public void update() {
        for(HomeObserver o : observers) o.updateHome(games);
    }
    public void addObserver(HomeObserver observer) {
        this.observers.add(observer);
    }
    public void removeObserver(HomeObserver observer) {
        this.observers.remove(observer);
    }
}
