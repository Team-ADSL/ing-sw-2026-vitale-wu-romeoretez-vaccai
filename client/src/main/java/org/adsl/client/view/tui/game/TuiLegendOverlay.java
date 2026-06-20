package org.adsl.client.view.tui.game;

import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiText;
import org.adsl.client.view.tui.render.TuiTextGraphics;

/**
 * Static symbol-legend overlay (toggled with the L key): a boxed panel pinned to
 * the top-right corner listing the meaning of every glyph used on the board.
 */
public final class TuiLegendOverlay {

    private TuiLegendOverlay() {}

    private static final String[][] ENTRIES = {
        {"─── SYMBOLS ───────────────────────"},
        {"🌟", "Prestige Points (PP)"},
        {"★", " Shaman ritual stars"},
        {"💰", "Food cost"},
        {"🍖", "Extra food on pick"},
        {"🗿", "Totem symbol"},
        {"🛡️", " Immunity"},
        {"🏁", "End-game PP bonus"},
        {"🌈", "Set collection bonus"},
        {"🧍", "Character card"},
        {" I II III", "Card Era"},
    };

    /** Draws the legend box in the top-right corner of the screen. */
    public static void draw(TuiTextGraphics tg, TuiSize sz) {
        int w = 38;
        int h = 14;
        int x = sz.getColumns() - w - 2;
        int y = 1;

        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.CYAN);
        String top = "┌" + "─".repeat(w - 2) + "┐";
        String bot = "└" + "─".repeat(w - 2) + "┘";
        tg.putString(x, y, top);
        tg.putString(x, y + h - 1, bot);
        for (int r = 1; r < h - 1; r++)
            tg.putString(x, y + r, "│" + " ".repeat(w - 2) + "│");

        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(x + 2, y, " LEGEND (L to close) ");

        tg.setForegroundColor(TuiColor.WHITE);
        int lineY = y + 2;
        for (String[] entry : ENTRIES) {
            if (entry.length == 1) {
                tg.setForegroundColor(TuiColor.CYAN);
                tg.putString(x + 2, lineY, TuiText.padRight(entry[0], w - 4));
                tg.setForegroundColor(TuiColor.WHITE);
            } else {
                int tokenW = TuiText.visualWidth(entry[0]);
                boolean hasFE0F = entry[0].indexOf('️') >= 0;
                String sep = hasFE0F ? "" : " ";
                int descW = hasFE0F ? (w - 4 - tokenW) : (w - 5 - tokenW);
                tg.putString(x + 2, lineY, entry[0] + sep + TuiText.padRight(entry[1], descW));
            }
            lineY++;
            if (lineY >= y + h - 1) break;
        }
        tg.setBackgroundColor(TuiColor.BLACK);
    }
}
