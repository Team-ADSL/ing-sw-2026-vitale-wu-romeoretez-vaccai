package org.adsl.client.view.tui.render;

/**
 * Semantic keystroke produced by {@link TuiTerminal#pollInput()}, replacing
 * Lanterna's {@code KeyStroke}. Only the key categories actually consumed by
 * the TUI event layer are modelled.
 */
public final class Key {

    public enum Type {
        CHARACTER,
        ENTER,
        ARROW_UP,
        ARROW_DOWN,
        ARROW_LEFT,
        ARROW_RIGHT,
        ESCAPE,
        BACKSPACE
    }

    private final Type type;
    private final char character;

    private Key(Type type, char character) {
        this.type = type;
        this.character = character;
    }

    /** @return the category of this keystroke */
    public Type getType() { return type; }

    /**
     * @return the typed character. Only meaningful when {@link #getType()}
     *         is {@link Type#CHARACTER}; other key types carry {@code '\0'}.
     */
    public char getCharacter() { return character; }

    /** @return a {@link Type#CHARACTER} key wrapping {@code c} */
    public static Key character(char c) { return new Key(Type.CHARACTER, c); }
    /** @return an {@link Type#ENTER} key */
    public static Key enter()           { return new Key(Type.ENTER, '\0'); }
    /** @return an {@link Type#ARROW_UP} key */
    public static Key arrowUp()         { return new Key(Type.ARROW_UP, '\0'); }
    /** @return an {@link Type#ARROW_DOWN} key */
    public static Key arrowDown()       { return new Key(Type.ARROW_DOWN, '\0'); }
    /** @return an {@link Type#ARROW_LEFT} key */
    public static Key arrowLeft()       { return new Key(Type.ARROW_LEFT, '\0'); }
    /** @return an {@link Type#ARROW_RIGHT} key */
    public static Key arrowRight()      { return new Key(Type.ARROW_RIGHT, '\0'); }
    /** @return an {@link Type#ESCAPE} key */
    public static Key escape()          { return new Key(Type.ESCAPE, '\0'); }
    /** @return a {@link Type#BACKSPACE} key */
    public static Key backspace()       { return new Key(Type.BACKSPACE, '\0'); }
}
