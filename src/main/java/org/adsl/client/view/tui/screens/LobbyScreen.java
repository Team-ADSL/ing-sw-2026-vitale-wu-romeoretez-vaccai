package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.Screen;

import java.io.IOException;
import java.util.List;

/**
 * Renders the lobby screen directly to the Lanterna Screen.
 * Shows players as they join the game. Updated each time onLobbyUpdate() is called.
 */
public class LobbyScreen {
    private final Screen screen;

    public LobbyScreen(Screen screen) {
        this.screen = screen;
    }

    public void render(List<String> players, int totalPlayers) {
        try {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            TerminalSize size = screen.getTerminalSize();
            int cols = size.getColumns();

            // ── Header ───────────────────────────────────────────────────────
            fillRow(tg, 0, cols, TextColor.ANSI.YELLOW, TextColor.ANSI.BLACK, ' ');
            tg.setForegroundColor(TextColor.ANSI.BLACK);
            tg.setBackgroundColor(TextColor.ANSI.YELLOW);
            putCentered(tg, 0, cols, "  M E S O S  –  Game Lobby  ");

            tg.setForegroundColor(TextColor.ANSI.WHITE);
            tg.setBackgroundColor(TextColor.ANSI.BLACK);

            // ── Status ───────────────────────────────────────────────────────
            int row = 2;
            tg.putString(4, row++, "Waiting for players to join...");
            tg.putString(4, row++, String.format("(%d / %d players ready)", players.size(), totalPlayers));
            row++;

            // ── Player list ───────────────────────────────────────────────────
            tg.putString(4, row++, "Players:");
            row++;
            for (int i = 0; i < totalPlayers; i++) {
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

            // ── Note ─────────────────────────────────────────────────────────
            row += 2;
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            tg.putString(4, row, "Game will start automatically when all players have joined.");
            tg.setForegroundColor(TextColor.ANSI.WHITE);

            screen.refresh();
        } catch (IOException e) {
            System.err.println("Error rendering lobby: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

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
