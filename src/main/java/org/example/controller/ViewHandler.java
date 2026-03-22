package org.example.controller;

import org.example.model.game.Game;

public interface ViewHandler {
    public void updateView(Game game);
    public void sendValidActions();
    public void showErrorMessage(String s);
}
