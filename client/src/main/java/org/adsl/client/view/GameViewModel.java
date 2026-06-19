package org.adsl.client.view;

import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.model.PlayerDTO;

import java.util.Map;

/**
 * View-agnostic read model over a {@link GameDTO} snapshot for the local player.
 * Holds the pure, transport-/toolkit-free game queries that both the TUI and GUI
 * {@code GameScreen}s need (whose turn it is, the picking phase, how many cards
 * the current offer tile grants, player-name lookups, phase formatting). Keeping
 * this logic in one place removes the duplication between the two screens and
 * makes it unit-testable without JavaFX or JLine.
 *
 * <p>Instances are cheap and immutable; build a fresh one whenever the snapshot
 * changes. Transient UI state (e.g. "waiting for server") stays in the screen,
 * which composes it with these queries.
 */
public final class GameViewModel {

    /** Cards a player may pick from the top and low rows on their current turn. */
    public record MoveCounts(int upper, int lower) {}

    private final GameDTO game;
    private final String username;

    public GameViewModel(GameDTO game, String username) {
        this.game = game;
        this.username = username;
    }

    public GameDTO game() { return game; }

    public boolean hasGame() { return game != null; }

    public Phase phase() { return game == null ? null : game.phase(); }

    /** This client's player in the snapshot, or {@code null} if not present. */
    public PlayerDTO me() {
        if (game == null || username == null || game.players() == null) return null;
        for (PlayerDTO p : game.players()) {
            if (username.equals(p.name())) return p;
        }
        return null;
    }

    /** Display name of the player holding {@code totem}, or {@code "?"}. */
    public String playerName(Totem totem) {
        if (game == null || totem == null || game.players() == null) return "?";
        for (PlayerDTO p : game.players()) {
            if (p.totem() == totem) return p.name();
        }
        return "?";
    }

    public boolean isMyTurn() {
        PlayerDTO me = me();
        return me != null && me.totem() == game.currentPlayerTotem();
    }

    public boolean isTotemPlacement() { return phase() == Phase.TOTEM_PLACEMENT; }

    public boolean isExtraMove() { return phase() == Phase.EXTRA_MOVE; }

    /** True during the two card-picking phases ({@code ACTION_EXECUTION} / {@code EXTRA_MOVE}). */
    public boolean isPicking() {
        Phase ph = phase();
        return ph == Phase.ACTION_EXECUTION || ph == Phase.EXTRA_MOVE;
    }

    /**
     * Stable id for the current picking turn ({@code phase:totem}), or
     * {@code "none"} outside the card-picking phases. A change signals a new
     * turn so the screen can drop a stale pending selection.
     */
    public String pickingTurnToken() {
        if (!isPicking()) return "none";
        Totem cur = game.currentPlayerTotem();
        return phase().name() + ":" + (cur == null ? "?" : cur.name());
    }

    /**
     * How many top/low-row cards the current player may pick. EXTRA_MOVE grants a
     * single optional pick from the top row; otherwise the count comes from the
     * offer tile the current totem sits on.
     */
    public MoveCounts moveCounts() {
        if (game == null) return new MoveCounts(0, 0);
        if (phase() == Phase.EXTRA_MOVE) return new MoveCounts(1, 0);
        Totem current = game.currentPlayerTotem();
        if (current == null || game.board() == null) return new MoveCounts(0, 0);
        for (OfferTileDTO tile : game.board().offerTrack()) {
            if (tile.totem() == current) {
                Map<Row, Integer> moves = tile.moves();
                if (moves == null) return new MoveCounts(0, 0);
                return new MoveCounts(moves.getOrDefault(Row.UPPER, 0),
                                      moves.getOrDefault(Row.LOWER, 0));
            }
        }
        return new MoveCounts(0, 0);
    }

    /** "ACTION_EXECUTION" -> "Action Execution"; {@code null} -> "—". */
    public static String formatPhase(Phase phase) {
        if (phase == null) return "—";
        StringBuilder sb = new StringBuilder();
        for (String word : phase.name().split("_")) {
            if (sb.length() > 0) sb.append(' ');
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)));
                sb.append(word.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }
}
