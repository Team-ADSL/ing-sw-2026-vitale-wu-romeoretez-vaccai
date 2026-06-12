package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.ConfirmEvent;
import org.adsl.client.view.tui.events.NavigateDownEvent;
import org.adsl.client.view.tui.events.NavigateLeftEvent;
import org.adsl.client.view.tui.events.NavigateRightEvent;
import org.adsl.client.view.tui.events.NavigateUpEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;

import java.io.IOException;

/**
 * Shown whenever the server connection is lost. Presents the error message and
 * two actions:
 * <ul>
 *   <li><b>Reconnect</b> – calls {@link AppCoordinator#connectRequest()} and
 *       transitions to {@link ConnectingScreen}.</li>
 *   <li><b>Exit</b> – transitions to {@link ExitScreen} to close the application.</li>
 * </ul>
 *
 * Replaces the original Lanterna blocking-dialog implementation with manual
 * ANSI rendering plus arrow-key focus, integrated into the standard TUI
 * event loop (no nested run loop, no widget framework).
 */
public class DisconnectedScreen extends TUIScreen {

    private static final int FIELD_RECONNECT = 0;
    private static final int FIELD_EXIT      = 1;
    private static final int FIELD_COUNT     = 2;

    private final String message;
    private int focus = FIELD_RECONNECT;
    private String localError;

    /**
     * @param terminal    the TUI terminal used for rendering
     * @param coordinator the coordinator used to attempt reconnection
     * @param message     optional server-supplied disconnect reason shown to the user
     */
    public DisconnectedScreen(TuiTerminal terminal,
                              AppCoordinator coordinator,
                              String message) {
        super(terminal, coordinator);
        this.message = message;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        TuiSize size = terminal.getTerminalSize();
        int cols = size.getColumns();
        int rows = size.getRows();

        int boxWidth  = 52;
        int boxHeight = 11;
        int x0 = Math.max(0, (cols - boxWidth) / 2);
        int y0 = Math.max(0, (rows - boxHeight) / 2);

        drawBox(tg, x0, y0, boxWidth, boxHeight, " MESOS – Disconnected ");

        int innerX = x0 + 2;
        int row = y0 + 2;

        tg.setForegroundColor(TuiColor.RED);
        tg.putString(innerX, row++, "Connection to the server was lost.");
        tg.setForegroundColor(TuiColor.WHITE);
        if (message != null && !message.isBlank()) {
            tg.putString(innerX, row++, truncate(message, boxWidth - 4));
        }
        if (localError != null) {
            tg.setForegroundColor(TuiColor.RED);
            tg.putString(innerX, row++, truncate("! " + localError, boxWidth - 4));
            tg.setForegroundColor(TuiColor.WHITE);
        }
        row++;

        drawButton(tg, innerX,                row, "  Reconnect  ",         focus == FIELD_RECONNECT);
        drawButton(tg, innerX + 18,           row, "  Exit Application  ",  focus == FIELD_EXIT);

        row = y0 + boxHeight - 2;
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(innerX, row, "← → / ↑ ↓ to navigate · ENTER to confirm");
        tg.setForegroundColor(TuiColor.WHITE);

        terminal.refresh();
    }

    // ── Input events ──────────────────────────────────────────────────────────

    @Override public TUIScreen visit(NavigateLeftEvent e)  { focus = (focus - 1 + FIELD_COUNT) % FIELD_COUNT; return this; }
    @Override public TUIScreen visit(NavigateRightEvent e) { focus = (focus + 1) % FIELD_COUNT;               return this; }
    @Override public TUIScreen visit(NavigateUpEvent e)    { focus = (focus - 1 + FIELD_COUNT) % FIELD_COUNT; return this; }
    @Override public TUIScreen visit(NavigateDownEvent e)  { focus = (focus + 1) % FIELD_COUNT;               return this; }

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        if (focus == FIELD_EXIT) {
            return ExitScreen.INSTANCE;
        }
        try {
            appCoordinator.reconnect();
            return new ConnectingScreen(terminal, appCoordinator);
        } catch (Exception ex) {
            localError = "Could not reconnect: " + ex.getMessage();
            return this;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void drawBox(TuiTextGraphics tg, int x, int y, int w, int h, String title) {
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
        StringBuilder top    = new StringBuilder("┌");
        StringBuilder bottom = new StringBuilder("└");
        for (int i = 0; i < w - 2; i++) {
            top.append('─');
            bottom.append('─');
        }
        top.append('┐');
        bottom.append('┘');

        tg.putString(x, y, top.toString());
        tg.putString(x, y + h - 1, bottom.toString());
        for (int i = 1; i < h - 1; i++) {
            tg.putString(x, y + i, "│");
            tg.putString(x + w - 1, y + i, "│");
            tg.putString(x + 1, y + i, " ".repeat(w - 2));
        }
        if (title != null && !title.isEmpty()) {
            int tx = x + Math.max(1, (w - title.length()) / 2);
            tg.setForegroundColor(TuiColor.YELLOW);
            tg.putString(tx, y, title);
            tg.setForegroundColor(TuiColor.WHITE);
        }
    }

    private void drawButton(TuiTextGraphics tg, int x, int y, String label, boolean focused) {
        if (focused) {
            tg.setBackgroundColor(TuiColor.WHITE);
            tg.setForegroundColor(TuiColor.BLACK);
        } else {
            tg.setForegroundColor(TuiColor.WHITE);
            tg.setBackgroundColor(TuiColor.BLACK);
        }
        tg.putString(x, y, label);
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return (s.length() <= max) ? s : s.substring(0, max - 1) + "…";
    }
}
