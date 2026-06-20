package org.adsl.client.view.tui.game;

import org.adsl.client.view.GameViewModel;
import org.adsl.client.view.tui.CardCatalog;
import org.adsl.client.view.tui.CardTokens;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiText;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.CardToken;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.model.OrderCellDTO;
import org.adsl.shared.utils.Move;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stateless renderer for the central game board (header, card rows, offer track,
 * order tile and the cursors that float over them). It draws a single immutable
 * {@link BoardFrame} snapshot produced by the screen, so it owns no state and
 * never reads back from the controller — mirroring the GUI's
 * {@code BoardRenderer} separation.
 */
public final class TuiBoardRenderer {

    private TuiBoardRenderer() {}

    /** Inner content width of a card box (the drawn box is {@code CARD_W + 2} wide). */
    public static final int CARD_W = 15;

    /** Static display label per game phase, looked up declaratively (no branching). */
    private static final Map<Phase, String> PHASE_LABELS = new EnumMap<>(Map.of(
            Phase.TOTEM_PLACEMENT,  "PLACE TOTEM",
            Phase.ACTION_EXECUTION, "PICK CARDS",
            Phase.EXTRA_MOVE,       "EXTRA PICK",
            Phase.EVENTS_EXECUTION, "EVENTS",
            Phase.END_ROUND,        "END OF ROUND",
            Phase.END_GAME,         "GAME OVER",
            Phase.TOTEM_PICKING,    "TOTEM PICKING"));

    /**
     * Immutable per-frame description of everything the board renderer needs.
     * The screen rebuilds one of these each render pass; the renderer only reads.
     *
     * @param game            the game snapshot to draw
     * @param vm              shared read model (player-name lookups)
     * @param myTotem         the local player's totem, may be {@code null}
     * @param highlights      card moves to render as selected
     * @param offerCursorIndex offer-track tile to mark, or {@code -1} for none
     * @param cardCursor      the picking cursor, or {@code null} when not picking
     * @param rowFocus        the passive row marker, or {@code null} when picking
     * @param topRowOffset    horizontal scroll offset of the top card row
     * @param lowRowOffset    horizontal scroll offset of the bottom card row
     */
    public record BoardFrame(
            GameDTO game,
            GameViewModel vm,
            Totem myTotem,
            Set<Move> highlights,
            int offerCursorIndex,
            CardCursor cardCursor,
            RowFocus rowFocus,
            int topRowOffset,
            int lowRowOffset) {

        /** Active picking cursor over a card row. */
        public record CardCursor(int index, int offset, boolean onTopRow) {}

        /** Passive marker shown on the row being viewed when not actively picking. */
        public enum RowFocus { TOP, BOTTOM }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Draws the whole board block (header, three rows, offer track, cursors). */
    public static void render(TuiTextGraphics tg, TuiSize sz, BoardFrame f) {
        int cols = sz.getColumns();
        GameDTO game = f.game();
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        drawHeader(tg, cols, game, f.vm());

        int topRowY = 2;
        drawSectionLabel(tg, topRowY, cols, "TOP ROW");
        drawCardRow(tg, game.board().topRow(), topRowY + 1, f.highlights(), Row.UPPER, cols, f.topRowOffset());

        int offerTrackY = 8;
        drawSectionLabel(tg, offerTrackY, cols, "OFFER TRACK", turnOrderText(game));
        drawOfferTrack(tg, game.board().offerTrack(), offerTrackY + 1, f.offerCursorIndex(), cols, f.vm());

        int botRowY = 14;
        drawSectionLabel(tg, botRowY, cols, "BOTTOM ROW");
        drawCardRow(tg, game.board().lowRow(), botRowY + 1, f.highlights(), Row.LOWER, cols, f.lowRowOffset());

        BoardFrame.CardCursor cc = f.cardCursor();
        if (cc != null) {
            int cursorRow = cc.onTopRow() ? topRowY + 1 : botRowY + 1;
            highlightCursor(tg, cc.index(), cursorRow, cc.offset(), cols);
        }
        if (f.offerCursorIndex() >= 0) {
            highlightOfferCursor(tg, f.offerCursorIndex(), offerTrackY + 1);
        }
        if (f.rowFocus() != null) {
            drawRowViewCursor(tg, topRowY, botRowY, f.rowFocus());
        }
    }

    /** Bottom controls bar; the screen builds the hint text and passes it in. */
    public static void drawControls(TuiTextGraphics tg, TuiSize sz, String hint) {
        int row = sz.getRows() - 1;
        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.WHITE);
        String line = "  " + hint;
        tg.putString(0, row, line + " ".repeat(Math.max(0, sz.getColumns() - line.length())));
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    /** Section divider with a left label and an optional right label. Public so the
     *  deck inspector can reuse the exact same heading style. */
    public static void drawSectionLabel(TuiTextGraphics tg, int row, int cols, String leftLabel, String rightLabel) {
        tg.setForegroundColor(TuiColor.CYAN);
        if (rightLabel == null || rightLabel.isEmpty()) {
            String line = " ── " + leftLabel + " " + "─".repeat(Math.max(0, cols - leftLabel.length() - 6));
            tg.putString(0, row, line.substring(0, Math.min(line.length(), cols)));
        } else {
            int dashCount = cols - leftLabel.length() - rightLabel.length() - 9;
            if (dashCount < 1) dashCount = 1;
            String line = " ── " + leftLabel + " " + "─".repeat(dashCount) + " " + rightLabel + " ──";
            tg.putString(0, row, line.substring(0, Math.min(line.length(), cols)));
        }
        tg.setForegroundColor(TuiColor.WHITE);
    }

    /** Section divider with a left label only. */
    public static void drawSectionLabel(TuiTextGraphics tg, int row, int cols, String label) {
        drawSectionLabel(tg, row, cols, label, null);
    }

    /**
     * Draws one row of cards starting at {@code offset}, line by line so every
     * output line is a single {@code putString}. Public so the deck inspector
     * reuses it for the per-player deck panel.
     */
    public static void drawCardRow(TuiTextGraphics tg, List<CardDTO> cards, int startRow,
            Set<Move> highlights, Row rowType, int cols, int offset) {
        String border = "┌" + "─".repeat(CARD_W) + "┐";
        String borderBot = "└" + "─".repeat(CARD_W) + "┘";

        StringBuilder topLine    = new StringBuilder("  ");
        StringBuilder typeLine   = new StringBuilder("  ");
        StringBuilder effectLine = new StringBuilder("  ");
        StringBuilder botLine    = new StringBuilder("  ");

        String dim   = TuiColor.DARK_GRAY.fg();
        String reset = TuiColor.WHITE.fg();

        // Card stride: box (CARD_W+2) + 1 gap = CARD_W+3 columns per card. The right
        // border of card at rendered-index n sits at 0-based column
        //   2 (initial indent) + n*(CARD_W+3) + (CARD_W+1)
        // We jump there with "\033[NG" before printing │ so it lands at the correct
        // column regardless of how the terminal rendered the emoji content.
        int count = 0;
        int start = Math.max(0, offset);
        for (int i = start; i < cards.size(); i++) {
            if (2 + (count + 1) * (CARD_W + 3) > cols - 1) break;

            String jumpRight = "\033[" + (2 + count * (CARD_W + 3) + CARD_W + 2) + "G";

            CardDTO card = cards.get(i);
            if (card == null) {
                topLine.append(dim).append(border).append(reset).append(" ");
                typeLine.append(dim).append("│").append(" ".repeat(CARD_W)).append(jumpRight).append("│").append(reset).append(" ");
                effectLine.append(dim).append("│").append(" ".repeat(CARD_W)).append(jumpRight).append("│").append(reset).append(" ");
                botLine.append(dim).append(borderBot).append(reset).append(" ");
            } else {
                boolean highlighted = highlights.contains(new Move(i, rowType));
                CardType type = CardCatalog.typeFromId(card.id());
                TuiColor cardColor = TuiColor.WHITE;
                if (type == CardType.BUILDINGS)     cardColor = TuiColor.MAGENTA;
                else if (CardCatalog.isEvent(type)) cardColor = TuiColor.ORANGE;
                TuiColor c = highlighted ? TuiColor.GREEN : cardColor;

                String rawTl = CardTokens.toEmoji(card.typeLabel() != null ? card.typeLabel() : "");
                String costStr = card.costLabel() != null ? CardTokens.toEmoji(card.costLabel()) : "";
                String combined = rawTl.isEmpty() ? costStr
                        : costStr.isEmpty() ? rawTl
                        : (TuiText.visualWidth(rawTl) + 1 + TuiText.visualWidth(costStr) <= CARD_W)
                                ? rawTl + " " + costStr
                                : rawTl + costStr;
                String tl = TuiText.padRight(TuiText.padLeft(combined, (CARD_W + TuiText.visualWidth(combined)) / 2), CARD_W);
                String rawEl = CardTokens.toEffectLabel(card.effectsLabel() != null ? card.effectsLabel() : "");
                String el = TuiText.padRight(TuiText.padLeft(rawEl, (CARD_W + TuiText.visualWidth(rawEl)) / 2), CARD_W);

                topLine.append(c.fg()).append(border).append(TuiColor.WHITE.fg()).append(" ");
                typeLine.append(c.fg()).append("│").append(tl).append(jumpRight).append("│").append(TuiColor.WHITE.fg()).append(" ");
                effectLine.append(c.fg()).append("│").append(el).append(jumpRight).append("│").append(TuiColor.WHITE.fg()).append(" ");
                botLine.append(c.fg()).append(borderBot).append(TuiColor.WHITE.fg()).append(" ");
            }
            count++;
        }

        tg.setForegroundColor(TuiColor.DARK_GRAY);
        tg.putString(0, startRow,     topLine.toString());
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        tg.putString(0, startRow + 1, typeLine.toString());
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        tg.putString(0, startRow + 2, effectLine.toString());
        tg.setForegroundColor(TuiColor.DARK_GRAY);
        tg.putString(0, startRow + 3, botLine.toString());
        tg.setForegroundColor(TuiColor.WHITE);
    }

    // ── Internal draw helpers ───────────────────────────────────────────────────

    private static void drawHeader(TuiTextGraphics tg, int cols, GameDTO game, GameViewModel vm) {
        tg.setForegroundColor(TuiColor.BLACK);
        tg.setBackgroundColor(TuiColor.YELLOW);
        String header = String.format("  MESOS  │  Round %d/10  │  Era %d  │  Phase: %-18s │  Current: %s  ",
                game.round(), game.era(), phaseLabel(game.phase()), currentPlayerLabel(game, vm));
        if (header.length() < cols) header += " ".repeat(cols - header.length());
        tg.putString(0, 0, header.substring(0, Math.min(header.length(), cols)));
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }

    private static String turnOrderText(GameDTO game) {
        if (game.board().orderTile() == null) return "Turn Order: ";
        List<OrderCellDTO> cells = game.board().orderTile().cells();
        if (cells == null) return "Turn Order: ";
        StringBuilder sb = new StringBuilder("Turn Order: ");
        for (int i = 0; i < cells.size(); i++) {
            OrderCellDTO cell = cells.get(i);
            if (cell.totem() != null) {
                sb.append(CardCatalog.totemLabel(cell.totem()));
            } else if (cell.isMalus()) {
                sb.append("-1").append(CardTokens.toEmoji(CardToken.FOOD))
                  .append("/-2").append(CardTokens.toEmoji(CardToken.PP));
            } else if (cell.bonus() > 0) {
                sb.append("+").append(cell.bonus()).append(CardTokens.toEmoji(CardToken.FOOD));
            } else {
                sb.append("--");
            }
            if (i < cells.size() - 1) sb.append(" → ");
        }
        return sb.toString();
    }

    private static void drawOfferTrack(TuiTextGraphics tg, List<OfferTileDTO> tiles,
            int startRow, int cursorIndex, int cols, GameViewModel vm) {
        int col = 2;
        for (int i = 0; i < tiles.size(); i++) {
            OfferTileDTO tile = tiles.get(i);
            boolean selected = (i == cursorIndex);

            if (selected) {
                tg.setForegroundColor(TuiColor.YELLOW);
            } else if (tile.totem() != null) {
                tg.setForegroundColor(CardCatalog.totemColor(tile.totem()));
            } else {
                tg.setForegroundColor(TuiColor.WHITE);
            }

            String playerName = (tile.totem() != null) ? vm.playerName(tile.totem()) : "";
            String movesLabel = formatMovesLabel(tile);

            tg.putString(col, startRow, "┌──────────┐");
            tg.putString(col, startRow + 1, "│ " + TuiText.padRight(TuiText.padLeft(playerName, (8 + TuiText.visualWidth(playerName)) / 2), 8) + " │");
            tg.putString(col, startRow + 2, "│ " + TuiText.padRight(TuiText.padLeft(movesLabel, (8 + TuiText.visualWidth(movesLabel)) / 2), 8) + " │");
            tg.putString(col, startRow + 3, "└──────────┘");
            tg.setForegroundColor(TuiColor.WHITE);
            col += 14;
            if (col + 14 > cols) break;
        }
    }

    private static void highlightCursor(TuiTextGraphics tg, int index, int rowY, int offset, int cols) {
        int visibleIndex = index - offset;
        if (visibleIndex < 0) return;
        int step = CARD_W + 3;
        int col = 2 + visibleIndex * step + (CARD_W + 2) / 2;
        if (col >= cols) return;
        if (2 + (visibleIndex + 1) * (CARD_W + 3) > cols - 1) return;
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(col, rowY - 1, "▼");
        tg.setForegroundColor(TuiColor.WHITE);
    }

    private static void highlightOfferCursor(TuiTextGraphics tg, int index, int rowY) {
        int col = 2 + index * 14 + 5;
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(col, rowY - 1, "▼");
        tg.setForegroundColor(TuiColor.WHITE);
    }

    private static String formatMovesLabel(OfferTileDTO tile) {
        if (tile.givesFood())
            return "+3" + CardTokens.toEmoji(CardToken.FOOD);
        Map<Row, Integer> moves = tile.moves();
        if (moves == null)
            return "—";
        int up = moves.getOrDefault(Row.UPPER, 0);
        int lo = moves.getOrDefault(Row.LOWER, 0);
        if (up == 0 && lo == 0)
            return "—";
        return "▲".repeat(up) + "▼".repeat(lo);
    }

    private static void drawRowViewCursor(TuiTextGraphics tg, int topRowY, int botRowY, BoardFrame.RowFocus focus) {
        boolean top = focus == BoardFrame.RowFocus.TOP;
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(0, topRowY, top ? "►" : " ");
        tg.putString(0, botRowY, top ? " " : "►");
        tg.setForegroundColor(TuiColor.WHITE);
    }

    private static String phaseLabel(Phase phase) {
        return phase == null ? "---" : PHASE_LABELS.getOrDefault(phase, "---");
    }

    private static String currentPlayerLabel(GameDTO game, GameViewModel vm) {
        Totem t = game.currentPlayerTotem();
        if (t == null) return "---";
        return vm.playerName(t) + " [" + CardCatalog.totemLabel(t) + "]";
    }
}
