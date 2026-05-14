package org.adsl.client.view.tui.render;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * Thin abstraction over a JLine {@link Terminal} that mirrors the subset of
 * the Lanterna {@code Screen} API used by the existing TUI screens:
 * {@link #clear()}, {@link #getTerminalSize()}, {@link #newTextGraphics()},
 * {@link #refresh()} and non-blocking input via {@link #pollInput()}.
 *
 * Frame-buffered rendering: {@link TuiTextGraphics} appends ANSI commands
 * into a shared {@link StringBuilder}; the buffer is flushed to the terminal
 * in one write per {@link #refresh()} call to avoid visible repaint flicker.
 *
 * Input is decoded with JLine's {@link BindingReader} fed by a {@link KeyMap}
 * built from the terminfo capabilities of the active terminal, so arrow
 * keys, ENTER and BACKSPACE resolve correctly on every supported platform
 * (Unix VT, macOS, Windows Console / Terminal) without hand-rolled
 * escape-sequence parsing.
 */
public class TuiTerminal {

    private enum KeyOp { UP, DOWN, LEFT, RIGHT, ENTER, BACKSPACE, CHAR }

    private final Terminal terminal;
    private final PrintWriter writer;
    private final BindingReader bindingReader;
    private final KeyMap<KeyOp> keyMap;
    private final StringBuilder buffer = new StringBuilder(8192);
    private final Attributes savedAttributes;
    private boolean closed = false;

    public TuiTerminal() throws IOException {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        this.writer = terminal.writer();
        this.savedAttributes = terminal.enterRawMode();

        this.bindingReader = new BindingReader(terminal.reader());
        this.keyMap = buildKeyMap(terminal);

        // Enter the alternate screen buffer and hide the cursor for a clean
        // full-screen experience; both are reverted in close().
        writer.write(AnsiCodes.ALT_SCREEN_ON);
        writer.write(AnsiCodes.HIDE_CURSOR);
        writer.flush();
    }

    private static KeyMap<KeyOp> buildKeyMap(Terminal t) {
        KeyMap<KeyOp> m = new KeyMap<>();
        // Arrows: bind both the terminfo-reported sequence and the standard
        // ANSI fallback (\e[A/B/C/D), in case the terminal lacks a terminfo
        // entry. Windows Terminal/Console emit slightly different codes
        // depending on the mode; pulling the sequence from the running
        // terminal via KeyMap.key avoids hard-coding any single variant.
        bindArrow(m, t, KeyOp.UP,    InfoCmp.Capability.key_up,    "[A");
        bindArrow(m, t, KeyOp.DOWN,  InfoCmp.Capability.key_down,  "[B");
        bindArrow(m, t, KeyOp.RIGHT, InfoCmp.Capability.key_right, "[C");
        bindArrow(m, t, KeyOp.LEFT,  InfoCmp.Capability.key_left,  "[D");

        m.bind(KeyOp.ENTER, "\r", "\n");
        m.bind(KeyOp.BACKSPACE, "\b", String.valueOf((char) 0x7F));

        // Any printable Unicode char that is not part of a bound sequence
        // is delivered as CHAR; same for byte sequences that ultimately do
        // not match any binding.
        m.setUnicode(KeyOp.CHAR);
        m.setNomatch(KeyOp.CHAR);
        // Short ambiguous-prefix wait so a bare ESC press does not stall the
        // event loop for the BindingReader's default one second.
        m.setAmbiguousTimeout(50L);
        return m;
    }

    private static void bindArrow(KeyMap<KeyOp> m, Terminal t, KeyOp op,
                                  InfoCmp.Capability cap, String ansiTail) {
        String seq = KeyMap.key(t, cap);
        String esc = String.valueOf((char) 0x1B);
        String ansi = esc + ansiTail;
        if (seq != null && !seq.isEmpty() && !seq.equals(ansi)) {
            m.bind(op, seq);
        }
        m.bind(op, ansi);
    }

    public TuiSize getTerminalSize() {
        int cols = terminal.getWidth();
        int rows = terminal.getHeight();
        // Some terminals report 0 before the first SIGWINCH; fall back to a
        // sensible default rather than divide-by-zero downstream.
        if (cols <= 0) cols = 80;
        if (rows <= 0) rows = 24;
        return new TuiSize(cols, rows);
    }

    public void clear() {
        buffer.append(AnsiCodes.RESET);
        buffer.append(AnsiCodes.CLEAR_SCREEN);
    }

    public TuiTextGraphics newTextGraphics() {
        return new TuiTextGraphics(buffer);
    }

    public void refresh() {
        if (buffer.length() == 0) return;
        writer.write(buffer.toString());
        writer.flush();
        buffer.setLength(0);
    }

    /**
     * Reads one semantic key with up to {@code timeoutMillis} of waiting.
     * Returns {@code null} on timeout (no input available).
     */
    public Key pollInput(long timeoutMillis) throws IOException {
        int peek = bindingReader.peekCharacter(timeoutMillis);
        if (peek < 0) return null;       // timeout / EOF
        KeyOp op = bindingReader.readBinding(keyMap);
        if (op == null) return null;

        return switch (op) {
            case UP        -> Key.arrowUp();
            case DOWN      -> Key.arrowDown();
            case LEFT      -> Key.arrowLeft();
            case RIGHT     -> Key.arrowRight();
            case ENTER     -> Key.enter();
            case BACKSPACE -> Key.backspace();
            case CHAR -> {
                String seq = bindingReader.getLastBinding();
                if (seq == null || seq.isEmpty()) yield null;
                char c = seq.charAt(0);
                // Drop control characters (lone ESC, stray bytes, etc.).
                if (c < 0x20 || c == 0x7F) yield null;
                yield Key.character(c);
            }
        };
    }

    /** Convenience: poll with a short timeout, suitable for the main loop. */
    public Key pollInput() throws IOException {
        return pollInput(20L);
    }

    public synchronized void close() {
        if (closed) return;
        closed = true;
        try {
            writer.write(AnsiCodes.RESET);
            writer.write(AnsiCodes.SHOW_CURSOR);
            writer.write(AnsiCodes.ALT_SCREEN_OFF);
            writer.flush();
        } finally {
            try {
                terminal.setAttributes(savedAttributes);
            } catch (Exception ignored) {}
            try {
                terminal.close();
            } catch (IOException ignored) {}
        }
    }
}