package org.adsl.client.view.tui.screens;

import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.events.ConfirmEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.model.MatchResult;

import java.io.IOException;
import java.util.List;

/**
 * Displays the final leaderboard. On {@link ConfirmEvent} (Enter) transitions
 * to {@link ExitScreen} to terminate the TUI.
 */
public class EndGameScreen extends Screen {

    private final List<MatchResult> results;

    public EndGameScreen(TuiTerminal terminal, List<MatchResult> results) {
        super(terminal);
        this.results = results;
    }

    @Override
    public void render() throws IOException {
        terminal.clear();
        TuiTextGraphics tg = terminal.newTextGraphics();
        TuiSize sz = terminal.getTerminalSize();
        int cols = sz.getColumns();

        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.YELLOW);
        tg.putString(0, 0, " ".repeat(cols));
        putCentered(tg, 0, cols, "  M E S O S  –  G A M E  O V E R  ");
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        int row = 2;
        tg.setForegroundColor(TuiColor.CYAN);
        putCentered(tg, row++, cols, "Final Standings");
        row++;

        tg.setForegroundColor(TuiColor.YELLOW);
        String colHeader = String.format("  %-4s  %-20s  %s", "Rank", "Player", "Prestige Points");
        putCentered(tg, row++, cols, colHeader);
        putCentered(tg, row++, cols, "─".repeat(Math.min(colHeader.length(), cols - 4)));
        tg.setForegroundColor(TuiColor.WHITE);

        String[] medals = {"🥇", "🥈", "🥉"};
        for (int i = 0; i < results.size(); i++) {
            MatchResult r = results.get(i);
            String medal = (i < medals.length) ? medals[i] : "   ";
            String line = String.format("  %s  %-20s  %d PP", medal, r.nickname(), r.score());
            tg.setForegroundColor(i == 0 ? TuiColor.YELLOW : TuiColor.WHITE);
            putCentered(tg, row++, cols, line);
        }

        if (results.size() > 1 && results.get(0).score() == results.get(1).score()) {
            row++;
            tg.setForegroundColor(TuiColor.CYAN);
            putCentered(tg, row, cols, "Tie broken by most Food tokens.");
        }

        tg.setForegroundColor(TuiColor.WHITE);
        putCentered(tg, sz.getRows() - 2, cols, "Press ENTER or [B] to exit.");
        terminal.refresh();
    }

    @Override
    public Screen visit(ConfirmEvent e) {
        return ExitScreen.INSTANCE;
    }

    @Override
    public Screen visit(CharInputEvent e) {
        if (Character.toLowerCase(e.getCharacter()) == 'b') {
            return ExitScreen.INSTANCE;
        }
        return this;
    }

    private void putCentered(TuiTextGraphics tg, int row, int cols, String text) {
        int col = Math.max(0, (cols - text.length()) / 2);
        tg.putString(col, row, text);
    }
}
