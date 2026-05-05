package org.adsl.client.view.tui.render;

/**
 * Central repository of ANSI escape sequence fragments used by the TUI
 * rendering layer. Keeping ESC-prefixed strings in one place avoids
 * scattering literal escape characters across the codebase.
 */
public final class AnsiCodes {

    private AnsiCodes() {}

    /** Single ESC character (0x1B). */
    public static final String ESC = String.valueOf((char) 0x1B);

    /** Control Sequence Introducer: {@code ESC [}. */
    public static final String CSI = ESC + "[";

    /** Reset all SGR attributes to defaults. */
    public static final String RESET = CSI + "0m";

    /** Erase the entire screen and move cursor to home position. */
    public static final String CLEAR_SCREEN = CSI + "2J" + CSI + "H";

    /** Hide the text cursor. */
    public static final String HIDE_CURSOR = CSI + "?25l";

    /** Show the text cursor. */
    public static final String SHOW_CURSOR = CSI + "?25h";

    /** Switch to the alternate screen buffer (preserves user shell on exit). */
    public static final String ALT_SCREEN_ON = CSI + "?1049h";

    /** Restore the primary screen buffer. */
    public static final String ALT_SCREEN_OFF = CSI + "?1049l";

    /** Move cursor to {@code (row, col)} using 1-based ANSI coordinates. */
    public static String moveCursor(int row, int col) {
        return CSI + row + ";" + col + "H";
    }
}
