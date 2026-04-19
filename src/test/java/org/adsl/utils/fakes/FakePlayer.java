package org.adsl.utils.fakes;

import org.adsl.server.model.Player;

public class FakePlayer extends Player {
    private final String name;
    private boolean active = true;

    public FakePlayer(String name) {
        super(name);
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
