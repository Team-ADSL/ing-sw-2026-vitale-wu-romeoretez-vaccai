package org.adsl.client.view.tui.game;

import org.adsl.client.view.tui.CardCatalog;
import org.adsl.client.view.tui.render.TuiColor;
import org.adsl.client.view.tui.render.TuiSize;
import org.adsl.client.view.tui.render.TuiTextGraphics;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.PlayerDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bottom-area overlay (toggled with the C key) that shows one player's full
 * collection as card boxes. Owns its own navigation state — open flag, selected
 * player and horizontal scroll offset — so the screen no longer carries it.
 * {@code ↑/↓} switch player, {@code A/D} scroll, {@code Q/E} jump to the ends.
 */
public final class TuiDeckInspector {

    private boolean open = false;
    private int playerIndex = 0;
    private int offset = 0;

    public boolean isOpen() {
        return open;
    }

    /** Toggles the overlay, resetting the view to the first player when opening. */
    public void toggle() {
        open = !open;
        if (open) {
            playerIndex = 0;
            offset = 0;
        }
    }

    // ── Navigation ──────────────────────────────────────────────────────────────

    public void playerPrev() {
        if (playerIndex > 0) {
            playerIndex--;
            offset = 0;
        }
    }

    public void playerNext(GameDTO game) {
        if (playerIndex < playerList(game).size() - 1) {
            playerIndex++;
            offset = 0;
        }
    }

    public void scrollLeft() {
        offset = Math.max(0, offset - 1);
    }

    public void scrollRight(GameDTO game, int visible) {
        int max = Math.max(0, currentDeck(game).size() - visible);
        offset = Math.min(max, offset + 1);
    }

    public void jumpStart() {
        offset = 0;
    }

    public void jumpEnd(GameDTO game, int visible) {
        offset = Math.max(0, currentDeck(game).size() - visible);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /**
     * Draws the deck panel at {@code startRow}, reusing {@link TuiBoardRenderer}'s
     * card-row and section-label styling so it matches the board exactly.
     *
     * @param visible number of card slots that fit on screen (computed by the screen)
     */
    public void draw(TuiTextGraphics tg, TuiSize sz, int startRow, GameDTO game, Totem myTotem, int visible) {
        int cols = sz.getColumns();
        List<PlayerDTO> players = playerList(game);
        if (players.isEmpty()) return;
        if (playerIndex >= players.size()) playerIndex = players.size() - 1;
        PlayerDTO p = players.get(playerIndex);

        boolean isMe = (p.totem() == myTotem);
        String who = p.name() + (isMe ? " (you)" : "")
                + " [" + CardCatalog.totemLabel(p.totem()) + "]"
                + "  " + (playerIndex + 1) + "/" + players.size();
        TuiBoardRenderer.drawSectionLabel(tg, startRow, cols, "DECK: " + who,
                "↑↓ player · A/D scroll · Q/E ends · C close");

        List<CardDTO> deck = currentDeck(game);
        if (deck.isEmpty()) {
            tg.setForegroundColor(TuiColor.DARK_GRAY);
            tg.putString(2, startRow + 2, "(no cards yet)");
            tg.setForegroundColor(TuiColor.WHITE);
            return;
        }

        int max = Math.max(0, deck.size() - visible);
        if (offset > max) offset = max;

        TuiBoardRenderer.drawCardRow(tg, deck, startRow + 1, Collections.emptySet(), Row.UPPER, cols, offset);

        // Active-row marker on the left, plus edge hints when more cards exist.
        tg.setForegroundColor(TuiColor.YELLOW);
        tg.putString(0, startRow + 2, "►");
        if (offset > 0) tg.putString(1, startRow + 2, "«");
        if (offset < max) tg.putString(cols - 1, startRow + 2, "»");
        tg.setForegroundColor(TuiColor.WHITE);
    }

    // ── Model helpers ───────────────────────────────────────────────────────────

    /** Players in a stable totem order so the deck index always maps to the same player. */
    private List<PlayerDTO> playerList(GameDTO game) {
        List<PlayerDTO> list = new ArrayList<>(game.players());
        list.sort(Comparator.comparingInt(
                p -> p.totem() == null ? Integer.MAX_VALUE : p.totem().ordinal()));
        return list;
    }

    /** Flat, type-ordered list of the currently inspected player's cards. */
    private List<CardDTO> currentDeck(GameDTO game) {
        List<PlayerDTO> players = playerList(game);
        if (players.isEmpty()) return Collections.emptyList();
        int idx = Math.min(playerIndex, players.size() - 1);
        return deckCards(players.get(idx));
    }

    private static List<CardDTO> deckCards(PlayerDTO p) {
        List<CardDTO> out = new ArrayList<>();
        Map<CardType, Set<CardDTO>> cards = p.cards();
        if (cards == null) return out;
        for (CardType t : CardType.values()) {
            Set<CardDTO> set = cards.get(t);
            if (set != null) out.addAll(set);
        }
        return out;
    }
}
