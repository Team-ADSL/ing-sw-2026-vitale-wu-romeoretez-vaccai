package org.adsl.client.view.tui.render;

/**
 * Drop-in replacement for Lanterna's {@code TextGraphics} that emits ANSI
 * escape sequences into a shared {@link StringBuilder} owned by the
 * {@link TuiTerminal}. The terminal flushes the buffer once per
 * {@link TuiTerminal#refresh()} so that an entire frame is written in a
 * single I/O burst.
 *
 * Coordinates are 0-based to match the existing screens (which were written
 * against Lanterna). The class translates them to ANSI's 1-based system
 * internally.
 */
public class TuiTextGraphics {

    private final StringBuilder out;
    private TuiColor fg = TuiColor.DEFAULT;
    private TuiColor bg = TuiColor.DEFAULT;

    TuiTextGraphics(StringBuilder out) {
        this.out = out;
    }

    /**
     * Sets the foreground color for subsequent {@link #putString} calls.
     * Emits the ANSI code only when the color actually changes.
     *
     * @param color the new foreground color, or {@code null} for {@link TuiColor#DEFAULT}
     */
    public void setForegroundColor(TuiColor color) {
        if (color == null) color = TuiColor.DEFAULT;
        if (color != fg) {
            out.append(color.fg());
            fg = color;
        }
    }

    /**
     * Sets the background color for subsequent {@link #putString} calls.
     * Emits the ANSI code only when the color actually changes.
     *
     * @param color the new background color, or {@code null} for {@link TuiColor#DEFAULT}
     */
    public void setBackgroundColor(TuiColor color) {
        if (color == null) color = TuiColor.DEFAULT;
        if (color != bg) {
            out.append(color.bg());
            bg = color;
        }
    }

    /**
     * Writes {@code text} starting at column {@code col}, row {@code row}
     * (both 0-based, matching the legacy Lanterna API). Does nothing if
     * {@code text} is {@code null} or empty.
     *
     * @param col  0-based column to start writing at
     * @param row  0-based row to write to
     * @param text the text to write
     */
    public void putString(int col, int row, String text) {
        if (text == null || text.isEmpty()) return;
        out.append(AnsiCodes.moveCursor(row + 1, col + 1));
        out.append(text);
    }
}
