package org.adsl.client.view.tui.game;

import org.adsl.client.view.tui.CardTokens;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiText;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.model.CardToken;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Full-screen TUI reference overlays: the paged rulebook viewer and the static
 * summary card. Both are pure drawers — the screen owns the toggle/scroll state
 * and passes it in. Extracted from {@code GameScreen} so its body stays focused
 * on the live board.
 */
public final class TuiCardWindows {

    private TuiCardWindows() {}

    /** Reads the rules file and splits it into pages on the separator lines. */
    public static List<String> loadRulesPages() {
        List<String> pages = new ArrayList<>();
        try {
            var stream = TuiCardWindows.class.getResourceAsStream("/assets/rules/tui_textual_rules");
            if (stream == null) {
                pages.add("(Rules file not found.)");
                return pages;
            }
            String full = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            // Split on separator lines that look like: "--- PAGE N of M: ... ---"
            String[] parts = full.split("(?m)^---\\s+PAGE\\s+\\d+.*?---\\s*$");
            for (String part : parts) {
                String trimmed = part.strip();
                if (!trimmed.isEmpty()) pages.add(trimmed);
            }
            if (pages.isEmpty()) pages.add(full.strip());
        } catch (Exception ex) {
            pages.add("(Error loading rules: " + ex.getMessage() + ")");
        }
        return pages;
    }

    /**
     * Draws the rules overlay as a large centered box (≈88% wide, ≈80% tall).
     * The current page text is word-wrapped to fit the inner width. The board
     * is still visible on the edges behind the overlay.
     *
     * @return the (clamped) scroll offset actually used, for the caller to store back.
     */
    public static int drawRules(TuiTextGraphics tg, TuiSize sz, List<String> pages,
                                int pageIndex, int scrollOffset) {
        if (pages == null || pages.isEmpty()) return scrollOffset;

        int cols = sz.getColumns();
        int rows = sz.getRows();

        // Panel dimensions — large but not fullscreen so the board shows on edges.
        // Capped at 160 width and 43 height to avoid huge empty spaces but allow fullscreen text.
        int w = Math.min(160, Math.max(40, cols * 80 / 100));
        int h = Math.min(43, Math.max(14, rows * 75 / 100));
        int x = (cols - w) / 2;
        int y = (rows - h) / 2;

        // Draw panel background (fill with spaces to erase board content).
        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.WHITE);
        for (int r = y; r < y + h && r < rows; r++) {
            tg.putString(x, r, " ".repeat(w));
        }

        // Box border in cyan.
        tg.setForegroundColor(TuiColor.CYAN);
        String hbar = "─".repeat(w - 2);
        tg.putString(x,         y,         "┌" + hbar + "┐");
        tg.putString(x,         y + h - 1, "└" + hbar + "┘");
        for (int r = y + 1; r < y + h - 1; r++) {
            tg.putString(x,         r, "│");
            tg.putString(x + w - 1, r, "│");
        }

        // ── Title bar on row y+1: yellow background, black text ──────────────
        int pageNum   = pageIndex + 1;
        int pageTotal = pages.size();
        String titleLeft  = "  MESOS RULES  —  page " + pageNum + " of " + pageTotal + "  ";
        String titleRight = "  ← prev   → next  ";
        int fillLen = w - 2 - titleLeft.length() - titleRight.length();
        String titleRow = titleLeft + " ".repeat(Math.max(0, fillLen)) + titleRight;
        if (titleRow.length() > w - 2) titleRow = titleRow.substring(0, w - 2);
        tg.setBackgroundColor(TuiColor.YELLOW);
        tg.setForegroundColor(TuiColor.BLACK);
        tg.putString(x + 1, y + 1, titleRow);
        tg.setBackgroundColor(TuiColor.BLACK);

        // ── Separator on row y+2 ──────────────────────────────────────────────
        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(x, y + 2, "├" + "─".repeat(w - 2) + "┤");

        // ── Inner text area: rows y+3 to y+h-3, hint on y+h-2 ───────────────
        int innerW = w - 4;
        int innerH = h - 5;  // top + titlebar + separator + hintrow + bottom = 5 fixed rows
        int textX  = x + 2;
        int textY  = y + 3;

        String pageText = pages.get(Math.min(pageIndex, pages.size() - 1));
        List<String> lines = TuiText.wrapText(pageText, innerW);

        int maxScroll = Math.max(0, lines.size() - innerH);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        // Render content lines; section headers (ALL-CAPS) in orange, body in white.
        int lineY = textY;
        for (int i = scrollOffset; i < lines.size() && lineY < textY + innerH; i++, lineY++) {
            String line = lines.get(i);
            boolean isHeader = !line.isEmpty()
                    && line.equals(line.toUpperCase())
                    && line.matches("[A-Z0-9 /\\-()&,:!?'🌟🧍🏠🍖🏁]+")
                    && !line.matches("^[0-9].*");  // numbered building items stay white
            boolean isSubHeader = !line.isEmpty()
                    && line.equals(line.toUpperCase())
                    && line.matches("[ A-Z\\[\\]]+");  // numbered building items stay white
            if (isSubHeader) {
                tg.setForegroundColor(TuiColor.YELLOW);
            } else if (isHeader) {
                tg.setForegroundColor(TuiColor.ORANGE);
            } else {
                tg.setForegroundColor(TuiColor.WHITE);
            }
            tg.putString(textX, lineY, TuiText.padRight(line, innerW));
        }
        // Bottom hint line inside the box.
        if (scrollOffset < maxScroll) {
            tg.setForegroundColor(TuiColor.DARK_GRAY);
            tg.putString(x + 2, y + h - 2, " ... ");
        }

        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
        return scrollOffset;
    }

    public static void drawSummaryCard(TuiTextGraphics tg, TuiSize sz) {
        int cols = sz.getColumns();
        int rows = sz.getRows();

        int w = 55;
        int h = 28;
        int x = Math.max(0, (cols - w) / 2);
        int y = Math.max(0, (rows - h) / 2);

        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.WHITE);
        for (int r = y; r < y + h && r < rows; r++) {
            tg.putString(x, r, " ".repeat(w));
        }

        tg.setForegroundColor(TuiColor.CYAN);
        String hbar = "─".repeat(w - 2);
        tg.putString(x, y, "┌" + hbar + "┐");
        tg.putString(x, y + h - 1, "└" + hbar + "┘");
        for (int r = y + 1; r < y + h - 1; r++) {
            tg.putString(x, r, "│");
            tg.putString(x + w - 1, r, "│");
        }

        tg.setBackgroundColor(TuiColor.YELLOW);
        tg.setForegroundColor(TuiColor.BLACK);
        String title = "  SUMMARY CARD  ";
        int fillLen = w - 2 - title.length();
        String titleRow = " ".repeat(fillLen / 2) + title + " ".repeat(fillLen - fillLen / 2);
        tg.putString(x + 1, y + 1, titleRow);
        tg.setBackgroundColor(TuiColor.BLACK);

        tg.setForegroundColor(TuiColor.CYAN);
        tg.putString(x, y + 2, "├" + "─".repeat(w - 2) + "┤");

        int midX = x + w / 2;
        for (int r = y + 3; r < y + h - 1; r++) {
            tg.putString(midX, r, "│");
        }
        tg.putString(midX, y + 2, "┬");
        tg.putString(midX, y + h - 1, "┴");

        int colW = (w - 3) / 2;
        String evTitle = "EVENTS";
        String egTitle = "END GAME " + CardTokens.toEmoji(CardToken.ENDGAME);

        tg.setBackgroundColor(TuiColor.MAGENTA);
        tg.setForegroundColor(TuiColor.WHITE);
        tg.putString(x + 1 + (colW - evTitle.length()) / 2, y + 3, evTitle);

        tg.setBackgroundColor(TuiColor.RED);
        tg.setForegroundColor(TuiColor.WHITE);
        tg.putString(midX + 1 + (colW - TuiText.visualWidth(egTitle)) / 2, y + 3, egTitle);

        tg.setBackgroundColor(TuiColor.BLACK);
        tg.setForegroundColor(TuiColor.WHITE);

        int boxW = 22;
        int boxH = 5;
        int startY = y + 4;

        // MATRICE TESTI SINISTRA (EVENTS) - [box][riga]
        String[][] leftTexts = {
                {"HUNT(HNT)", "↓ ↓ ↓", "1🍖+N🌟 x HUN"},
                {"SUSTENANCE(SUS)", "↓ ↓ ↓", "-1🍖/N🌟 x 🧍"},
                {"SHAMANIC RITUAL(RIT)", "↓ ↓ ↓", ">★:N🌟 | <★:-N🌟"},
                {"CAVE PAINTINGS(PAI)", "↓ ↓ ↓", ">A:+N🌟xA|<A:-N🌟"}
        };

        // MATRICE TESTI DESTRA (END GAME) - [box][riga]
        String[][] rightTexts = {
                {"", "BLD: SUM🌟", ""}, // Box 1
                {"", "INV: 🌟 = INV x ICON", ""}, // Box 2
                {"", "ART: 10🌟 X 2ART", ""}, // Box 3
                {"", "BUI: 🏁 + SUM🌟", ""}  // Box 4
        };

        int leftX = x + 1 + (colW - boxW) / 2;
        int rightX = midX + 1 + (colW - boxW) / 2;

        for (int i = 0; i < 4; i++) {
            int boxY = startY + i * (boxH + 1);
            if (boxY + boxH > y + h - 1) break;

            drawCardBox(tg, leftX, boxY, boxW, boxH, leftTexts[i][0], leftTexts[i][1], leftTexts[i][2]);
            drawCardBox(tg, rightX, boxY, boxW, boxH, rightTexts[i][0], rightTexts[i][1], rightTexts[i][2]);
        }
    }

    private static void drawCardBox(TuiTextGraphics tg, int x, int y, int w, int h,
                                    String line1, String line2, String line3) {
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        String hbar = "─".repeat(w - 2);
        tg.putString(x, y, "┌" + hbar + "┐");
        tg.putString(x, y + h - 1, "└" + hbar + "┘");
        for (int r = y + 1; r < y + h - 1; r++) {
            tg.putString(x, r, "│");
            tg.putString(x + w - 1, r, "│");
        }

        // Stampa testi all'interno del box se non vuoti
        tg.setForegroundColor(TuiColor.WHITE);
        int innerW = w - 2;
        if (!line1.isEmpty()) tg.putString(x + 1, y + 1, TuiText.padRight(TuiText.padLeft(line1, (innerW + TuiText.visualWidth(line1)) / 2), innerW));
        if (!line2.isEmpty()) tg.putString(x + 1, y + 2, TuiText.padRight(TuiText.padLeft(line2, (innerW + TuiText.visualWidth(line2)) / 2), innerW));
        if (!line3.isEmpty()) tg.putString(x + 1, y + 3, TuiText.padRight(TuiText.padLeft(line3, (innerW + TuiText.visualWidth(line3)) / 2), innerW));
    }
}
