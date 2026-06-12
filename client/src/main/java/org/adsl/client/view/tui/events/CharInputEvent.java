package org.adsl.client.view.tui.events;

import org.adsl.client.view.tui.screens.TUIScreen;

/**
 * Printable character keystroke. Carries the typed character so text-input
 * screens can append it to the current input buffer.
 */
public class CharInputEvent extends InputEvent {
    private final char character;

    /**
     * Creates a printable-character input event.
     *
     * @param character the character that was typed
     */
    public CharInputEvent(char character) {
        this.character = character;
    }

    /**
     * @return the character that was typed
     */
    public char getCharacter() { return character; }

    @Override
    public TUIScreen accept(InputEventVisitor visitor) {
        return visitor.visit(this);
    }
}
