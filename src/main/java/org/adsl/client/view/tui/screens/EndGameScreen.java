package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import org.adsl.shared.model.MatchResult;

import java.io.IOException;
import java.util.List;

/**
 * Displays the final leaderboard at the end of the game and waits for ENTER.
 */
public class EndGameScreen {
    private final Screen screen;

    public EndGameScreen(Screen screen) {
        this.screen = screen;
    }

    public void show(List<MatchResult> results) {
        try {
            screen.clear();
            TextGraphics tg = screen.newTextGraphics();
            TerminalSize sz = screen.getTerminalSize();
            int cols = sz.getColumns();

            // Header
            tg.setForegroundColor(TextColor.ANSI.BLACK);
            tg.setBackgroundColor(TextColor.ANSI.YELLOW);
            putCentered(tg, 0, cols, "  MESOS  –  GAME OVER  ");
            tg.putString(0, 0, " ".repeat(cols));
            putCentered(tg, 0, cols, "  M E S O S  –  G A M E  O V E R  ");
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            tg.setBackgroundColor(TextColor.ANSI.BLACK);

            int row = 2;
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            putCentered(tg, row++, cols, "Final Standings");
            row++;

            // Column headers
            tg.setForegroundColor(TextColor.ANSI.YELLOW);
            String colHeader = String.format("  %-4s  %-20s  %s", "Rank", "Player", "Prestige Points");
            putCentered(tg, row++, cols, colHeader);
            putCentered(tg, row++, cols, "─".repeat(Math.min(colHeader.length(), cols - 4)));
            tg.setForegroundColor(TextColor.ANSI.WHITE);

            // Scores
            String[] medals = {"🥇", "🥈", "🥉"};
            for (int i = 0; i < results.size(); i++) {
                MatchResult r = results.get(i);
                String medal = (i < medals.length) ? medals[i] : "   ";
                String line = String.format("  %s  %-20s  %d PP", medal, r.nickname(), r.score());
                if (i == 0) tg.setForegroundColor(TextColor.ANSI.YELLOW);
                else        tg.setForegroundColor(TextColor.ANSI.WHITE);
                putCentered(tg, row++, cols, line);
            }

            // Tie-break note
            if (results.size() > 1 && results.get(0).score() == results.get(1).score()) {
                row++;
                tg.setForegroundColor(TextColor.ANSI.CYAN);
                putCentered(tg, row, cols, "Tie broken by most Food tokens.");
            }

            // Footer
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            putCentered(tg, sz.getRows() - 2, cols, "Press ENTER to exit.");
            screen.refresh();

            // Wait for ENTER
            KeyStroke key;
            do {
                key = screen.readInput();
            } while (key == null || key.getKeyType() != KeyType.Enter);

        } catch (IOException e) {
            System.err.println("Error rendering end game screen: " + e.getMessage());
        }
    }

    private void putCentered(TextGraphics tg, int row, int cols, String text) {
        int col = Math.max(0, (cols - text.length()) / 2);
        tg.putString(col, row, text);
    }
}
