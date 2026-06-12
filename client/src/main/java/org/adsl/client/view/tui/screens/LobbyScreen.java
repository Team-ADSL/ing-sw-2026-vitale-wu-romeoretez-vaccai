package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.serverEvents.GameUpdateEvent;
import org.adsl.client.serverEvents.LobbyUpdateEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;

import java.io.IOException;
import java.util.List;

/**
 * Shows the players in the lobby as they join. Updates in place when new
 * {@link LobbyUpdateEvent}s arrive. Transitions to {@link GameScreen} when
 * the server sends a {@link GameUpdateEvent} (game started).
 *
 * Input: {@link CharInputEvent} with 's' triggers startGameRequest (host only).
 */
public class LobbyScreen extends TUIScreen {

    private int gameId;
    private List<String> players;
    private final int totalPlayers;

    /**
     * @param terminal     the TUI terminal used for rendering
     * @param coordinator  the coordinator used to send start/exit-lobby requests
     * @param username     the current player's username
     * @param gameId       id of the game/lobby being displayed
     * @param players      usernames of players currently in the lobby, or {@code null}
     * @param totalPlayers number of players required to start, or 0 if unknown
     */
    public LobbyScreen(TuiTerminal terminal,
                       AppCoordinator coordinator,
                       String username,
                       int gameId,
                       List<String> players,
                       int totalPlayers) {
        super(terminal, coordinator, username);
        this.gameId = gameId;
        this.players = players != null ? players : List.of();
        this.totalPlayers = totalPlayers;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        TuiSize size = terminal.getTerminalSize();
        int cols = size.getColumns();

        fillRow(tg, 0, cols, TuiColor.YELLOW, TuiColor.BLACK, ' ');
        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.YELLOW);
        putCentered(tg, 0, cols, "  M E S O S  –  Game #" + gameId + "  ");

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        int row = 2;
        tg.putString(4, row++, "Waiting for players to join...");
        if (totalPlayers > 0) {
            tg.putString(4, row++, String.format("(%d / %d players ready)", players.size(), totalPlayers));
        } else {
            tg.putString(4, row++, String.format("(%d players in lobby)", players.size()));
        }
        row++;

        tg.putString(4, row++, "Players:");
        row++;
        int slots = (totalPlayers > 0) ? totalPlayers : players.size();
        for (int i = 0; i < slots; i++) {
            if (i < players.size()) {
                tg.setForegroundColor(TuiColor.GREEN);
                tg.putString(6, row, "✓  " + players.get(i));
                tg.setForegroundColor(TuiColor.WHITE);
            } else {
                tg.setForegroundColor(TuiColor.CYAN);
                tg.putString(6, row, "○  (waiting...)");
                tg.setForegroundColor(TuiColor.WHITE);
            }
            row++;
        }

        row += 2;
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(4, row++, "Press [S] to start the game when you are ready (host only).");
        tg.putString(4, row++, "Press [B] to go back.");
        tg.putString(4, row, "Press [M] to open the game log.");
        tg.setForegroundColor(TuiColor.WHITE);

        if (showLog) {
            drawLogWindow(tg, size);
        } else {
            drawLogPreview(tg, size);
        }

        if(error != null){
            row = size.getRows() - 2;
            tg.setForegroundColor(TuiColor.RED);
            tg.putString(2, row, "! " + error);
            tg.setForegroundColor(TuiColor.WHITE);
            error = null;
        }

        terminal.refresh();
    }

    // ── Input events ──────────────────────────────────────────────────────────

    @Override
    public TUIScreen visit(CharInputEvent e) {
        char ch = Character.toLowerCase(e.getCharacter());
        if (ch == 's') {
            try {
                appCoordinator.startGameRequest();
            } catch (Exception ex) {
                error = ex.getMessage();
            }
        } else if (ch == 'b') {
            try {
                appCoordinator.createExitLobbyRequest();
                // Server will reply with HomeUpdateEvent which routes to HomeScreen.
            } catch(Exception ex){
                error = ex.getMessage();
            }
        } else if (ch == 'm') {
            toggleLog();
        }
        return this;
    }

    // ── Server events ─────────────────────────────────────────────────────────

    @Override
    public TUIScreen visit(LobbyUpdateEvent e) {
        this.gameId = e.gameId();
        this.players = e.players() != null ? e.players() : List.of();
        return this;
    }



    // ── Helpers ───────────────────────────────────────────────────────────────

    private void fillRow(TuiTextGraphics tg, int row, int cols, TuiColor fg, TuiColor bg, char c) {
        tg.setForegroundColor(fg);
        tg.setBackgroundColor(bg);
        tg.putString(0, row, String.valueOf(c).repeat(cols));
    }

    private void putCentered(TuiTextGraphics tg, int row, int cols, String text) {
        int col = Math.max(0, (cols - text.length()) / 2);
        tg.putString(col, row, text);
    }
}
