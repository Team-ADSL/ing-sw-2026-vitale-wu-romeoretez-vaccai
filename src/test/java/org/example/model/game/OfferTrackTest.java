package org.example.model.game;
import org.example.controller.utils.Row;
import org.example.model.game.boardComponent.OfferTile;
import org.example.model.game.boardComponent.OfferTrack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class OfferTrackTest {

    private OfferTrack offerTrack;
    private OfferTile tile1;
    private OfferTile tile2;

    @BeforeEach
    void setUp() {
        // Arrange - create two tiles and add them to the track
        tile1 = new OfferTile(
                Optional.of(new Player("Gianpaolo", 3, 0, Color.RED)),
                Map.of(Row.UPPER, 1),
                false
        );
        tile2 = new OfferTile(
                Optional.empty(),
                Map.of(Row.LOWER, 2),
                true
        );

        ArrayList<OfferTile> tiles = new ArrayList<>();
        tiles.add(tile1);
        tiles.add(tile2);

        offerTrack = new OfferTrack(tiles);
    }

    // --- size() tests ---

    @Test
    void testSizeReturnsCorrectCount() {
        // should return the number of tiles in the track
        assertEquals(2, offerTrack.size());
    }

    @Test
    void testSizeEmptyTrack() {
        // empty track should have size 0
        OfferTrack emptyTrack = new OfferTrack(new ArrayList<>());
        assertEquals(0, emptyTrack.size());
    }

    // --- getTileAt() tests ---

    @Test
    void testGetTileAtReturnsCorrectTile() {
        // should return the tile at the given index
        assertEquals(tile1, offerTrack.getTileAt(0));
        assertEquals(tile2, offerTrack.getTileAt(1));
    }

    @Test
    void testGetTileAtThrowsOnInvalidIndex() {
        // should throw when index is out of bounds
        assertThrows(IndexOutOfBoundsException.class, () -> offerTrack.getTileAt(5));
    }

    // --- placeInOfferTile() tests ---

    @Test
    void testPlaceInOfferTileAssignsPlayer() {
        // should assign the player to the tile at the given index
        Player newPlayer = new Player("Gianpiero", 2, 0, Color.BLUE);
        offerTrack.placeInOfferTile(newPlayer, 1);
        assertTrue(offerTrack.getTileAt(1).getPlayer().isPresent());
        assertEquals(newPlayer, offerTrack.getTileAt(1).getPlayer().get());
    }

    @Test
    void testPlaceInOfferTileThrowsOnInvalidIndex() {
        // should throw when index is out of bounds
        Player newPlayer = new Player("Gianpiero", 2, 0, Color.BLUE);
        assertThrows(IndexOutOfBoundsException.class, () -> offerTrack.placeInOfferTile(newPlayer, 5));
    }
}
