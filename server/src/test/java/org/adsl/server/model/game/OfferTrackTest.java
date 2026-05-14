package org.adsl.server.model.game;
import org.adsl.shared.enums.Row;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.OfferTile;
import org.adsl.server.model.board.OfferTrack;
import org.adsl.shared.enums.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class OfferTrackTest {

    private OfferTrack offerTrack;
    private OfferTile tile1;
    private OfferTile tile2;

    @BeforeEach
    void setUp() {
        tile1 = new OfferTile("order_tile_5p",
                new Player("Gianpaolo", 3, 0, Totem.RED, new java.util.HashMap<>()),
                Map.of(Row.UPPER, 1),
                false
        );
        tile2 = new OfferTile("order_tile_5p",
                null,
                Map.of(Row.LOWER, 2),
                true
        );

        ArrayList<OfferTile> tiles = new ArrayList<>();
        tiles.add(tile1);
        tiles.add(tile2);

        offerTrack = new OfferTrack(tiles);
    }

    // ──────────────────────────────────────────────
    // TEST SIZE
    // ──────────────────────────────────────────────

    @Test
    void testSize_returnsCorrectCount() {
        assertEquals(2, offerTrack.size());
    }

    @Test
    void testSize_emptyTrack() {
        OfferTrack emptyTrack = new OfferTrack(new ArrayList<>());
        assertEquals(0, emptyTrack.size());
    }

    // ──────────────────────────────────────────────
    // TEST GET TILE AT
    // ──────────────────────────────────────────────

    @Test
    void testGetTileAt_returnsCorrectTile() {
        assertEquals(tile1, offerTrack.getTileAt(0));
        assertEquals(tile2, offerTrack.getTileAt(1));
    }

    @Test
    void testGetTileAt_throwsOnInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> offerTrack.getTileAt(5));
    }

    // ──────────────────────────────────────────────
    // TEST PLACE IN OFFER TILE
    // ──────────────────────────────────────────────

    @Test
    void testPlaceInOfferTile_assignsPlayer() {
        Player newPlayer = new Player("Gianpiero", 2, 0, Totem.BLUE, new java.util.HashMap<>());
        offerTrack.placeInOfferTile(newPlayer, 1);
        assertTrue(offerTrack.getTileAt(1).getPlayer().isPresent());
        assertEquals(newPlayer, offerTrack.getTileAt(1).getPlayer().get());
    }

    @Test
    void testPlaceInOfferTile_throwsOnInvalidIndex() {
        Player newPlayer = new Player("Gianpiero", 2, 0, Totem.BLUE, new java.util.HashMap<>());
        assertThrows(IndexOutOfBoundsException.class, () -> offerTrack.placeInOfferTile(newPlayer, 5));
    }
}
