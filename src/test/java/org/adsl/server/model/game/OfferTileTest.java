package org.adsl.server.model.game;
import org.adsl.shared.enums.Row;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.OfferTile;
import org.adsl.shared.enums.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class OfferTileTest {

    private OfferTile tileWithPlayer;
    private OfferTile tileWithoutPlayer;

    @BeforeEach
    void setUp() {
        tileWithPlayer = new OfferTile("order_tile_5p",
                new Player("Gianpaolo", 3, 0, Totem.RED, new java.util.HashMap<>()),
                Map.of(Row.UPPER, 2, Row.LOWER, 1),
                true
        );

        tileWithoutPlayer = new OfferTile("order_tile_5p",
                null,
                Map.of(),
                false
        );
    }

    // ──────────────────────────────────────────────
    // TEST GET PLAYER
    // ──────────────────────────────────────────────

    @Test
    void testGetPlayer_returnsPlayer() {
        assertTrue(tileWithPlayer.getPlayer().isPresent());
    }

    @Test
    void testGetPlayer_returnsEmpty() {
        assertTrue(tileWithoutPlayer.getPlayer().isEmpty());
    }

    // ──────────────────────────────────────────────
    // TEST GET NUM MOVES
    // ──────────────────────────────────────────────

    @Test
    void testGetNumMoves_returnsSumOfAllMoves() {
        assertEquals(3, tileWithPlayer.getNumMoves());
    }

    @Test
    void testGetNumMoves_returnsZeroWhenNoMoves() {
        assertEquals(0, tileWithoutPlayer.getNumMoves());
    }
}
