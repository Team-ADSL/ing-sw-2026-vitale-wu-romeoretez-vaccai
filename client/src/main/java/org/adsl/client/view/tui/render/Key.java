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

    public Type getType() { return type; }

    /** Only meaningful when {@link #getType()} is {@link Type#CHARACTER}. */
    public char getCharacter() { return character; }

    public static Key character(char c) { return new Key(Type.CHARACTER, c); }
    public static Key enter()           { return new Key(Type.ENTER, '\0'); }
    public static Key arrowUp()         { return new Key(Type.ARROW_UP, '\0'); }
    public static Key arrowDown()       { return new Key(Type.ARROW_DOWN, '\0'); }
    public static Key arrowLeft()       { return new Key(Type.ARROW_LEFT, '\0'); }
    public static Key arrowRight()      { return new Key(Type.ARROW_RIGHT, '\0'); }
    public static Key escape()          { return new Key(Type.ESCAPE, '\0'); }
    public static Key backspace()       { return new Key(Type.BACKSPACE, '\0'); }
}
