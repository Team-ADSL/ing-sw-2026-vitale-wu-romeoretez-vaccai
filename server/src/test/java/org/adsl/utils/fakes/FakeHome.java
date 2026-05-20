package org.adsl.utils.fakes;

import org.adsl.server.model.Home;
import org.adsl.server.network.HomeObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FakeHome extends Home {
    public boolean updateCalled = false;
    public List<HomeObserver> addedObservers = new ArrayList<>();

    @Override
    public void update() {
        this.updateCalled = true;
    }

    @Override
    public void update(String message) {
        this.updateCalled = true;
    }

    @Override
    public void update(Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity) {
        this.updateCalled = true;
    }

    @Override
    public void update(Map<Integer, List<String>> gamePlayers, Map<Integer, Integer> gameCapacity, String message) {
        this.updateCalled = true;
    }

    @Override
    public void addObserver(HomeObserver client) {
        addedObservers.add(client);
    }
}
