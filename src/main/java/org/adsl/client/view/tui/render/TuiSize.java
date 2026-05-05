package org.adsl.client.view.tui.render;

/**
 * Immutable terminal dimensions, mirroring the Lanterna {@code TerminalSize}
 * shape used throughout the TUI screens.
 */
public record TuiSize(int columns, int rows) {

    public int getColumns() { return columns; }

    public int getRows() { return rows; }
}
