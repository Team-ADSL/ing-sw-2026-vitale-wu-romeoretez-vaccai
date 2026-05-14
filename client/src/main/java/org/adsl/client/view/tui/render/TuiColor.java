package org.adsl.client.view.tui.render;

/**
 * Standard 8-color ANSI palette. Each constant carries the raw ANSI escape
 * sequences for foreground and background. Concrete drawing code uses
 * {@link #fg()} / {@link #bg()} to emit the right SGR codes.
 *
 * Exposed as a flat enum (not nested under an "ANSI" type as in Lanterna)
 * because the JLine-based TUI does not need true colors.
 */
public enum TuiColor {
    BLACK   (30),
    RED     (31),
    GREEN   (32),
    YELLOW  (33),
    BLUE    (34),
    MAGENTA (35),
    CYAN    (36),
    WHITE   (37),
    DEFAULT (39),
    DARK_GRAY (90),
    ORANGE  (0) {
        @Override
        public String fg() { return AnsiCodes.CSI + "38;5;208m"; }
        @Override
        public String bg() { return AnsiCodes.CSI + "48;5;208m"; }
    },
    DARK_PURPLE (0) {
        @Override
        public String fg() { return AnsiCodes.CSI + "38;5;54m"; }
        @Override
        public String bg() { return AnsiCodes.CSI + "48;5;54m"; }
    };

    private final int fgCode;

    TuiColor(int fgCode) {
        this.fgCode = fgCode;
    }

    /** ANSI SGR escape that sets this color as the foreground. */
    public String fg() {
        return AnsiCodes.CSI + fgCode + "m";
    }

    /** ANSI SGR escape that sets this color as the background. */
    public String bg() {
        return AnsiCodes.CSI + (fgCode + 10) + "m";
    }
}
