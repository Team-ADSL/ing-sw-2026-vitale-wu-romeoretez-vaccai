package org.adsl.client.view.tui.game;

import org.adsl.client.view.tui.CardCatalog;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiText;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.PlayerDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Stateless renderer for the per-player summary panels shown below the board:
 * the local player's tribe line plus the compact "OTHERS" rows. Card-detail
 * parsing lives in {@link TuiSummaryChunks}; this class only colours and places
 * the resulting chunks. Mirrors the GUI's {@code PlayerPanelsRenderer}.
 */
public final class TuiPlayerPanels {

    private TuiPlayerPanels() {}

    /** Draws the local player's tribe line (name, food, PP) and card-summary chunks. */
    public static void drawCurrentPlayerTribe(TuiTextGraphics tg, GameDTO game, Totem myTotem,
            int startRow, int cols) {
        Totem currentTotem = game.currentPlayerTotem();
        PlayerDTO me = game.players().stream()
                .filter(p -> p.totem() == myTotem)
                .findFirst()
                .orElse(game.players().isEmpty() ? null : game.players().iterator().next());
        if (me == null) return;

        boolean isMyTurn = (myTotem == currentTotem);
        String prefix = " ── ";
        String text = String.format("YOUR TRIBE: %s  Food: %d  PP: %d",
                me.name(), me.food(), me.pp());

        // Prefix dashes — totem color, never highlighted
        tg.setForegroundColor(CardCatalog.totemColor(me.totem()));
        tg.setBackgroundColor(TuiColor.BLACK);
        tg.putString(0, startRow, prefix);

        // Text — highlighted only on my turn
        if (isMyTurn) {
            tg.setBackgroundColor(CardCatalog.totemColor(me.totem()));
            tg.setForegroundColor(TuiColor.BLACK);
        } else {
            tg.setForegroundColor(CardCatalog.totemColor(me.totem()));
            tg.setBackgroundColor(TuiColor.BLACK);
        }
        int textCol = prefix.length();
        int textEnd = Math.min(cols, textCol + text.length());
        tg.putString(textCol, startRow,
                textEnd <= cols ? text : text.substring(0, cols - textCol));

        // Trailing dashes — totem color
        tg.setForegroundColor(CardCatalog.totemColor(me.totem()));
        tg.setBackgroundColor(TuiColor.BLACK);
        int trailCol = textCol + text.length();
        int trailLen = Math.max(0, cols - trailCol - 1);
        if (trailLen > 0) {
            tg.putString(trailCol, startRow, " " + "─".repeat(trailLen - 1));
        }
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);

        List<TuiSummaryChunks.Chunk> chunks = new ArrayList<>();
        TuiSummaryChunks.appendCharacterChunks(chunks, me);
        TuiSummaryChunks.appendBuildingsChunks(chunks, me);
        if (!chunks.isEmpty()) {
            renderChunks(tg, 0, startRow + 1, chunks, me, isMyTurn, cols);
        }
    }

    /** Draws the "OTHERS" header and one compact summary row per opponent. */
    public static void drawOtherPlayers(TuiTextGraphics tg, GameDTO game, Totem myTotem,
            int startRow, int cols) {
        tg.setForegroundColor(TuiColor.WHITE);
        tg.putString(0, startRow - 1, " ── OTHERS " + "─".repeat(Math.max(0, cols - 10)));

        Totem currentTotem = game.currentPlayerTotem();
        int row = startRow;
        for (PlayerDTO p : game.players()) {
            if (p.totem() == myTotem) continue;

            boolean isTheirTurn = (p.totem() == currentTotem);

            List<TuiSummaryChunks.Chunk> chunks = new ArrayList<>();
            chunks.add(new TuiSummaryChunks.Chunk(p.name(), true));
            chunks.add(new TuiSummaryChunks.Chunk(
                    String.format(" F:%d PP:%d %s ", p.food(), p.pp(), TuiSummaryChunks.SEP),
                    false));
            TuiSummaryChunks.appendCharacterChunks(chunks, p);
            TuiSummaryChunks.appendBuildingsChunks(chunks, p);
            renderChunks(tg, 0, row, chunks, p, isTheirTurn, cols, true);
            row++;
        }
    }

    /** Renders chunks left-to-right starting at {@code col}; highlighted chunks use totem color. */
    private static void renderChunks(TuiTextGraphics tg, int col, int row,
            List<TuiSummaryChunks.Chunk> chunks, PlayerDTO p,
            boolean isTheirTurn, int cols) {
        renderChunks(tg, col, row, chunks, p, isTheirTurn, cols, false);
    }

    private static void renderChunks(TuiTextGraphics tg, int col, int row,
            List<TuiSummaryChunks.Chunk> chunks, PlayerDTO p,
            boolean isTheirTurn, int cols,
            boolean tintNonHighlightedWithTotem) {
        TuiColor nonHighlightFg = tintNonHighlightedWithTotem
                ? CardCatalog.totemColor(p.totem()) : TuiColor.WHITE;
        for (TuiSummaryChunks.Chunk ch : chunks) {
            if (col >= cols) break;
            if (ch.highlight()) {
                if (isTheirTurn) {
                    tg.setBackgroundColor(CardCatalog.totemColor(p.totem()));
                    tg.setForegroundColor(TuiColor.BLACK);
                } else {
                    tg.setForegroundColor(CardCatalog.totemColor(p.totem()));
                    tg.setBackgroundColor(TuiColor.BLACK);
                }
            } else {
                tg.setForegroundColor(nonHighlightFg);
                tg.setBackgroundColor(TuiColor.BLACK);
            }
            String txt = ch.text();
            int w = TuiText.visualWidth(txt);
            if (col + w > cols) {
                txt = txt.substring(0, Math.min(txt.length(), cols - col));
                w = TuiText.visualWidth(txt);
            }
            tg.putString(col, row, txt);
            col += w;
        }
        tg.setForegroundColor(TuiColor.WHITE);
        tg.setBackgroundColor(TuiColor.BLACK);
    }
}
