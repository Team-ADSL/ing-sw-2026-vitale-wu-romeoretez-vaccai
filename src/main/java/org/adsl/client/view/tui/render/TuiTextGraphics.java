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

    public void setForegroundColor(TuiColor color) {
        if (color == null) color = TuiColor.DEFAULT;
        if (color != fg) {
            out.append(color.fg());
            fg = color;
        }
    }

    public void setBackgroundColor(TuiColor color) {
        if (color == null) color = TuiColor.DEFAULT;
        if (color != bg) {
            out.append(color.bg());
            bg = color;
        }
    }

    /**
     * Writes {@code text} starting at column {@code col}, row {@code row}
     * (both 0-based, matching the legacy Lanterna API).
     */
    public void putString(int col, int row, String text) {
        if (text == null || text.isEmpty()) return;
        out.append(AnsiCodes.moveCursor(row + 1, col + 1));
        out.append(text);
    }
}
