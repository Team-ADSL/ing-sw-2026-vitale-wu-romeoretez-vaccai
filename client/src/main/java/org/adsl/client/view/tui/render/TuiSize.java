package org.adsl.client.view.tui.render;

/**
 * Immutable terminal dimensions, mirroring the Lanterna {@code TerminalSize}
 * shape used throughout the TUI screens.
 *
 * @param columns number of character columns
 * @param rows    number of character rows
 */
public record TuiSize(int columns, int rows) {

    /** @return the number of character columns */
    public int getColumns() { return columns; }

    /** @return the number of character rows */
    public int getRows() { return rows; }
}
