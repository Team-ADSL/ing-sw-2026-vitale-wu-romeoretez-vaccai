package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.events.GameUpdateEvent;
import org.adsl.client.view.tui.events.LobbyUpdateEvent;

import java.io.IOException;
import java.util.List;

/**
 * Shows the players in the lobby as they join. Updates in place when new
 * {@link LobbyUpdateEvent}s arrive. Transitions to {@link GameScreen} when
 * the server sends a {@link GameUpdateEvent} (game started).
 *
 * Input: {@link CharInputEvent} with 's' triggers startGameRequest (host only).
 */
public class LobbyScreen implements Screen {

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final AppCoordinator coordinator;
    private final String username;
    private List<String> players;
    private final int totalPlayers;

    public LobbyScreen(com.googlecode.lanterna.screen.Screen terminal,
                       AppCoordinator coordinator,
                       String username,
                       List<String> players,
                       int totalPlayers) {
        this.terminal = terminal;
        this.coordinator = coordinator;
        this.username = username;
        this.players = players != null ? players : List.of();
        this.totalPlayers = totalPlayers;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TextGraphics tg = terminal.newTextGraphics();
        TerminalSize size = terminal.getTerminalSize();
        int cols = size.getColumns();

        fillRow(tg, 0, cols, TextColor.ANSI.YELLOW, TextColor.ANSI.BLACK, ' ');
        tg.setForegroundColor(TextColor.ANSI.BLACK);
        tg.setBackgroundColor(TextColor.ANSI.YELLOW);
        putCentered(tg, 0, cols, "  M E S O S  –  Game Lobby  ");

        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);

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
                tg.setForegroundColor(TextColor.ANSI.GREEN);
                tg.putString(6, row, "✓  " + players.get(i));
                tg.setForegroundColor(TextColor.ANSI.WHITE);
            } else {
                tg.setForegroundColor(TextColor.ANSI.CYAN);
                tg.putString(6, row, "○  (waiting...)");
                tg.setForegroundColor(TextColor.ANSI.WHITE);
            }
            row++;
        }

        row += 2;
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        tg.putString(4, row, "Press [S] to start the game when you are ready (host only).");
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        terminal.refresh();
    }

    // ── Input events ──────────────────────────────────────────────────────────

    @Override
    public Screen visit(CharInputEvent e) {
        if (Character.toLowerCase(e.getCharacter()) == 's') {
            try {
                coordinator.startGameRequest();
            } catch (Exception ex) {
                flashError("Start failed: " + ex.getMessage());
            }
        }
        return this;
    }

    // ── Server events ─────────────────────────────────────────────────────────

    @Override
    public Screen visit(LobbyUpdateEvent e) {
        this.players = e.getPlayers() != null ? e.getPlayers() : List.of();
        return this;
    }

    @Override
    public Screen visit(GameUpdateEvent e) {
        return new GameScreen(terminal, coordinator, username, e.getGame());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void flashError(String msg) {
        try {
            int row = terminal.getTerminalSize().getRows() - 2;
            TextGraphics tg = terminal.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.RED);
            tg.putString(2, row, "! " + msg);
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            terminal.refresh();
        } catch (IOException ignored) {}
    }

    private void fillRow(TextGraphics tg, int row, int cols, TextColor fg, TextColor bg, char c) {
        tg.setForegroundColor(fg);
        tg.setBackgroundColor(bg);
        tg.putString(0, row, String.valueOf(c).repeat(cols));
    }

    private void putCentered(TextGraphics tg, int row, int cols, String text) {
        int col = Math.max(0, (cols - text.length()) / 2);
        tg.putString(col, row, text);
    }
}
