package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

public class CharInputEvent extends InputEvent {
    private final char character;

    public CharInputEvent(char character) {
        this.character = character;
    }

    public char getCharacter() { return character; }

    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
