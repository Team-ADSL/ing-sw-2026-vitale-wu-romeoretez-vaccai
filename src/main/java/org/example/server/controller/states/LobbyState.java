package org.example.server.controller.states;

import org.example.server.model.Game;

public class LobbyState extends ControllerState {
    public LobbyState(Game game) {
        super(game);
    }

    @Override
    public ControllerState onEntry() {
        return null;
    }

    @Override
    public ControllerState nextState() {
        return null;
    }
}
