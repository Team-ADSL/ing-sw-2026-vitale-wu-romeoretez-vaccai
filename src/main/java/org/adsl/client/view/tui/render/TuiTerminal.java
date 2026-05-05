package org.adsl.client.view.tui.render;

import org.jline.terminal.Attributes;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.NonBlockingReader;

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
 */
public class TuiTerminal {

    private final Terminal terminal;
    private final PrintWriter writer;
    private final NonBlockingReader reader;
    private final StringBuilder buffer = new StringBuilder(8192);
    private final Attributes savedAttributes;
    private boolean closed = false;

    public TuiTerminal() throws IOException {
        this.terminal = TerminalBuilder.builder()
                .system(true)
                .build();
        this.writer = terminal.writer();
        this.reader = terminal.reader();
        this.savedAttributes = terminal.enterRawMode();

        // Enter the alternate screen buffer and hide the cursor for a clean
        // full-screen experience; both are reverted in close().
        writer.write(AnsiCodes.ALT_SCREEN_ON);
        writer.write(AnsiCodes.HIDE_CURSOR);
        writer.flush();
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
     *
     * Recognises: printable characters, ENTER, BACKSPACE, the four arrow
     * keys (interpreted from the standard {@code ESC [ A/B/C/D} sequence)
     * and bare ESC as its own key.
     */
    public Key pollInput(long timeoutMillis) throws IOException {
        int c = reader.read(timeoutMillis);
        if (c < 0) return null;

        if (c == 0x1B) { // ESC: possibly an arrow key sequence
            int next = reader.read(20L);
            if (next == '[') {
                int code = reader.read(20L);
                return switch (code) {
                    case 'A' -> Key.arrowUp();
                    case 'B' -> Key.arrowDown();
                    case 'C' -> Key.arrowRight();
                    case 'D' -> Key.arrowLeft();
                    default  -> Key.escape();
                };
            }
            return Key.escape();
        }
        if (c == '\r' || c == '\n') return Key.enter();
        if (c == 0x7F || c == 0x08) return Key.backspace();
        if (c >= 0x20 && c < 0x7F) return Key.character((char) c);
        // Treat other control chars (and high codepoints we do not handle) as no-op.
        return null;
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
