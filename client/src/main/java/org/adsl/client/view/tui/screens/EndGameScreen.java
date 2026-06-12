package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.CharInputEvent;
import org.adsl.client.view.tui.events.ConfirmEvent;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

/**
 * Displays the single-match standings on the left and the cumulative DB
 * leaderboard on the right; when the DB is unavailable the right column is
 * replaced by a red alert. On {@link ConfirmEvent} (Enter) transitions to
 * {@link ExitScreen} to terminate the TUI.
 */
public class EndGameScreen extends TUIScreen {

    private final List<MatchResult> results;
    private final List<DBRecord> records;
    private final String message;

    /**
     * @param terminal    the TUI terminal used for rendering
     * @param coordinator the coordinator used to send requests to the server
     * @param username    the current player's username
     * @param results     per-player results for this match, or {@code null} if the game ended before it started
     * @param records     overall leaderboard records from the database, or {@code null}/empty if unavailable
     * @param message     optional message shown when {@code records} is unavailable
     */
    public EndGameScreen(TuiTerminal terminal, AppCoordinator coordinator, String username,
                         List<MatchResult> results, List<DBRecord> records, String message) {
        super(terminal, coordinator, username);
        this.results = results;
        this.records = records;
        this.message = message;
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

        if (results == null && records == null) {
            tg.setForegroundColor(TuiColor.CYAN);
            putCentered(tg, sz.getRows() / 2, cols, "Game ended before it started.");
        } else {
            int leftStart = 2;
            int rightStart = cols / 2 + 2;
            int halfWidth = cols / 2 - 4;

            renderMatchBlock(tg, leftStart, halfWidth);
            renderDbBlock(tg, rightStart, halfWidth);
        }

        if (error != null) {
            tg.setForegroundColor(TuiColor.RED);
            putCentered(tg, sz.getRows() - 3, cols, error);
            error = null;
        }

        tg.setForegroundColor(TuiColor.WHITE);
        putCentered(tg, sz.getRows() - 2, cols, "Press [B] to return to Home, [M] to open the log, or ENTER to exit application.");

        if (showLog) {
            drawLogWindow(tg, sz);
        } else {
            drawLogPreview(tg, sz);
        }

        terminal.refresh();
    }

    private void renderMatchBlock(TuiTextGraphics tg, int col, int width) {
        int row = 2;
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(col, row++, "This Match");
        tg.putString(col, row++, "─".repeat(Math.max(1, Math.min(width, 22))));
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(col, row++, String.format("%-3s %-16s %4s %5s", "#", "Player", "PP", "Food"));
        tg.setForegroundColor(TuiColor.WHITE);

        if (results == null || results.isEmpty()) {
            tg.setForegroundColor(TuiColor.WHITE);
            tg.putString(col, row, "(no results)");
            return;
        }

        List<MatchResult> ranked = results.stream()
                .sorted(Comparator.comparingInt(MatchResult::pp).reversed()
                        .thenComparing(Comparator.comparingInt(MatchResult::food).reversed()))
                .toList();
        for (int i = 0; i < ranked.size(); i++) {
            MatchResult r = ranked.get(i);
            tg.setForegroundColor(i == 0 ? TuiColor.YELLOW : TuiColor.WHITE);
            tg.putString(col, row++, String.format("%-3d %-16s %4d %5d", i + 1, truncate(r.nickname(), 16), r.pp(), r.food()));
        }

        if (ranked.size() > 1 && ranked.get(0).pp() == ranked.get(1).pp()) {
            row++;
            tg.setForegroundColor(TuiColor.CYAN);
            tg.putString(col, row, "Tie broken by Food.");
        }
    }

    private void renderDbBlock(TuiTextGraphics tg, int col, int width) {
        int row = 2;
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(col, row++, "Overall Leaderboard");
        tg.putString(col, row++, "─".repeat(Math.max(1, Math.min(width, 22))));

        if (records == null || records.isEmpty()) {
            String alertText = (message != null && !message.isBlank())
                    ? message
                    : "Database unavailable.";
            tg.setForegroundColor(TuiColor.RED);
            for (String line : wrap(alertText, Math.max(20, width))) {
                tg.putString(col, row++, line);
            }
            return;
        }

        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(col, row++, String.format("%-4s %-16s %6s", "Rank", "Player", "Score"));
        tg.setForegroundColor(TuiColor.WHITE);
        for (int i = 0; i < records.size(); i++) {
            DBRecord r = records.get(i);
            tg.setForegroundColor(r.rank() == 1 ? TuiColor.YELLOW : TuiColor.WHITE);
            tg.putString(col, row++, String.format("%-4d %-16s %6d", r.rank(), truncate(r.nickname(), 16), r.score()));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private static List<String> wrap(String text, int width) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            if (cur.length() == 0) {
                cur.append(word);
            } else if (cur.length() + 1 + word.length() <= width) {
                cur.append(' ').append(word);
            } else {
                out.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        return ExitScreen.INSTANCE;
    }

    @Override
    public TUIScreen visit(CharInputEvent e) {
        char ch = Character.toLowerCase(e.getCharacter());
        if (ch == 'b') {
            if (appCoordinator != null) {
                try {
                    appCoordinator.createExitGameRequest();
                } catch (Exception ex) {
                    error = "Failed to return to home: " + ex.getMessage();
                }
            }
            return this;
        } else if (ch == 'm') {
            toggleLog();
        }
        return this;
    }

    private void putCentered(TuiTextGraphics tg, int row, int cols, String text) {
        int col = Math.max(0, (cols - text.length()) / 2);
        tg.putString(col, row, text);
    }
}
