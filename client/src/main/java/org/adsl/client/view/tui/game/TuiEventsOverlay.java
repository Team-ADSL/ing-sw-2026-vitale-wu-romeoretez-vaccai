package org.adsl.client.view.tui.game;

import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTextGraphics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Draws the TUI end-of-round events overlay: a hatched orange wash over the
 * board area with the event title and one centered line per player parsed from
 * the server log message. Stateless — the screen passes in the current title
 * and log message and this paints them into the buffer.
 */
public final class TuiEventsOverlay {

    private TuiEventsOverlay() {}

    public static void draw(TuiTextGraphics tg, TuiSize sz, String title, String log) {
        int cols = sz.getColumns();
        int rows = sz.getRows();
        if (cols <= 0 || rows <= 0) return;

        int top = 1;
        int bottom = Math.max(top, rows - 2);

        tg.setForegroundColor(TuiColor.ORANGE);
        tg.setBackgroundColor(TuiColor.BLACK);
        StringBuilder line = new StringBuilder(cols);
        for (int r = top; r <= bottom; r++) {
            line.setLength(0);
            for (int c = 0; c < cols; c++) {
                line.append(((c + r) % 2 == 0) ? '/' : ' ');
            }
            tg.putString(0, r, line.toString());
        }

        if (title == null) {
            tg.setForegroundColor(TuiColor.WHITE);
            tg.setBackgroundColor(TuiColor.BLACK);
            return;
        }

        String titleText = "  " + title.toUpperCase() + "  ";
        int titleW = titleText.length();
        int titleCol = Math.max(0, (cols - titleW) / 2);

        List<String> playerLines = parsePlayerLines(log);
        int blockHeight = 3 + (playerLines.isEmpty() ? 0 : playerLines.size() + 1);
        int blockTop = Math.max(top, (top + bottom) / 2 - blockHeight / 2);
        int titleRow = blockTop + 1;

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
        for (int r = blockTop; r < blockTop + blockHeight && r <= bottom; r++) {
            tg.putString(0, r, " ".repeat(cols));
        }
        tg.putString(titleCol, titleRow, titleText);

        int lineRow = titleRow + 2;
        for (String pl : playerLines) {
            if (lineRow > bottom) break;
            int col = Math.max(0, (cols - pl.length()) / 2);
            tg.putString(col, lineRow, pl);
            lineRow++;
        }

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    /**
     * Splits the server-side log message ("Title — Player1: +2 PP | Player2: no change …")
     * into the per-player segments after the em-dash separator. Empty if the
     * message is missing or doesn't follow the expected format — the overlay
     * then falls back to showing only the title.
     */
    private static List<String> parsePlayerLines(String log) {
        if (log == null || log.isBlank()) return Collections.emptyList();
        int sep = log.indexOf(" — ");
        if (sep < 0) return Collections.emptyList();
        String body = log.substring(sep + " — ".length());
        List<String> out = new ArrayList<>();
        for (String seg : body.split("\\s\\|\\s")) {
            String t = seg.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }
}
