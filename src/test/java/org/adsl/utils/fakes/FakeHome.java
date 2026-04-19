package org.adsl.utils.fakes;

import org.adsl.server.model.Home;
import org.adsl.server.network.HomeObserver;

import java.util.ArrayList;
import java.util.List;

public class FakeHome extends Home {
    public boolean updateCalled = false;
    public List<HomeObserver> addedObservers = new ArrayList<>();

    @Override
    public void update() {
        this.updateCalled = true;
    }

    @Override
    public void addObserver(HomeObserver client) {
        addedObservers.add(client);
    }
}
