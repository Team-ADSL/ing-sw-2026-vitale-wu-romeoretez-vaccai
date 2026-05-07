package org.adsl.client.view.events;

import org.adsl.client.view.gui.screens.GUIScreen;
import org.adsl.client.view.tui.events.EventVisitor;

import org.adsl.client.view.tui.screens.TUIScreen;
import org.adsl.shared.model.GameDTO;

public class GameUpdateEvent extends ServerEvent {
    private final GameDTO game;

    public GameUpdateEvent(GameDTO game) {
        this.game = game;
    }

    public GameDTO getGame() { return game; }

    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }

    @Override
    public GUIScreen accept(GUIEventVisitor visitor) {
        return visitor.visit(this);
    }
}
