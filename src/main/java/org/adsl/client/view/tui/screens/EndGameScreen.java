package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import org.adsl.client.view.tui.events.ConfirmEvent;
import org.adsl.shared.model.MatchResult;

import java.io.IOException;
import java.util.List;

/**
 * Displays the final leaderboard. On {@link ConfirmEvent} (Enter) transitions
 * to {@link ExitScreen} to terminate the TUI.
 */
public class EndGameScreen implements Screen {

    private final com.googlecode.lanterna.screen.Screen terminal;
    private final List<MatchResult> results;

    public EndGameScreen(com.googlecode.lanterna.screen.Screen terminal,
                         List<MatchResult> results) {
        this.terminal = terminal;
        this.results = results;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TextGraphics tg = terminal.newTextGraphics();
        TerminalSize sz = terminal.getTerminalSize();
        int cols = sz.getColumns();

        tg.setForegroundColor(TextColor.ANSI.BLACK);
        tg.setBackgroundColor(TextColor.ANSI.YELLOW);
        tg.putString(0, 0, " ".repeat(cols));
        putCentered(tg, 0, cols, "  M E S O S  –  G A M E  O V E R  ");
        tg.setForegroundColor(TextColor.ANSI.WHITE);
        tg.setBackgroundColor(TextColor.ANSI.BLACK);

        int row = 2;
        tg.setForegroundColor(TextColor.ANSI.CYAN);
        putCentered(tg, row++, cols, "Final Standings");
        row++;

        tg.setForegroundColor(TextColor.ANSI.YELLOW);
        String colHeader = String.format("  %-4s  %-20s  %s", "Rank", "Player", "Prestige Points");
        putCentered(tg, row++, cols, colHeader);
        putCentered(tg, row++, cols, "─".repeat(Math.min(colHeader.length(), cols - 4)));
        tg.setForegroundColor(TextColor.ANSI.WHITE);

        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < results.size(); i++) {
            MatchResult r = results.get(i);
            String medal = (i < medals.length) ? medals[i] : "   ";
            String line = String.format("  %s  %-20s  %d PP", medal, r.nickname(), r.score());
            tg.setForegroundColor(i == 0 ? TextColor.ANSI.YELLOW : TextColor.ANSI.WHITE);
            putCentered(tg, row++, cols, line);
        }

        if (results.size() > 1 && results.get(0).score() == results.get(1).score()) {
            row++;
            tg.setForegroundColor(TextColor.ANSI.CYAN);
            putCentered(tg, row, cols, "Tie broken by most Food tokens.");
        }

        tg.setForegroundColor(TextColor.ANSI.WHITE);
        putCentered(tg, sz.getRows() - 2, cols, "Press ENTER to exit.");
        terminal.refresh();
    }

    @Override
    public Screen visit(ConfirmEvent e) {
        return ExitScreen.INSTANCE;
    }

    private void putCentered(TextGraphics tg, int row, int cols, String text) {
        int col = Math.max(0, (cols - text.length()) / 2);
        tg.putString(col, row, text);
    }
}
