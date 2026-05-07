package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;
import org.adsl.shared.model.GameDTO;

public class GameUpdateEvent extends Event {
    private final GameDTO game;

    public GameUpdateEvent(GameDTO game) {
        this.game = game;
    }

    public GameDTO getGame() { return game; }

    @Override
    public TUIScreen accept(EventVisitor visitor) {
        return visitor.visit(this);
    }
}
