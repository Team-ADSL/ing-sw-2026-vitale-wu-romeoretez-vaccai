package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.Screen;
import org.adsl.shared.model.GameDTO;

public class GameUpdateEvent extends Event {
    private final GameDTO game;

    public GameUpdateEvent(GameDTO game) {
        this.game = game;
    }

    public GameDTO getGame() { return game; }

    @Override
    public Screen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
