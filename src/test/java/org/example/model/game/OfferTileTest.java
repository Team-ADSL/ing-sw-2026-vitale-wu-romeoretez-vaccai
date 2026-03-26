package org.example.model.game;
import org.example.shared.enums.Row;
import org.example.server.model.Player;
import org.example.server.model.board.OfferTile;
import org.example.shared.enums.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class OfferTileTest {

    private OfferTile tileWithPlayer;
    private OfferTile tileWithoutPlayer;

    @BeforeEach
    void setUp() {
        // Arrange - tile with a player and multiple moves
        tileWithPlayer = new OfferTile(
                Optional.of(new Player("Gianpaolo", 3, 0, Color.RED)),
                Map.of(Row.UPPER, 2, Row.LOWER, 1),
                true
        );

        // Arrange - tile with no player and no moves
        tileWithoutPlayer = new OfferTile(
                Optional.empty(),
                Map.of(),
                false
        );
    }

    // --- getPlayer() tests ---

    @Test
    void testGetPlayerReturnsPlayer() {
        // should return the player when present
        assertTrue(tileWithPlayer.getPlayer().isPresent());
    }

    @Test
    void testGetPlayerReturnsEmpty() {
        // should return empty when no player assigned
        assertTrue(tileWithoutPlayer.getPlayer().isEmpty());
    }

    // --- getNumMoves() tests ---

    @Test
    void testGetNumMovesReturnsSumOfAllMoves() {
        // should sum all move values across rows (2 + 1 = 3)
        assertEquals(3, tileWithPlayer.getNumMoves());
    }

    @Test
    void testGetNumMovesReturnsZeroWhenNoMoves() {
        // should return 0 when map is empty
        assertEquals(0, tileWithoutPlayer.getNumMoves());
    }

    // --- getMoves() tests ---

    @Test
    void testGetMovesReturnsCorrectMap() {
        // should return the exact map passed in constructor
        Map<Row, Integer> moves = tileWithPlayer.getMoves();
        assertEquals(2, moves.get(Row.UPPER));
        assertEquals(1, moves.get(Row.LOWER));
    }

    @Test
    void testGetMovesReturnsEmptyMap() {
        // should return empty map when no moves
        assertTrue(tileWithoutPlayer.getMoves().isEmpty());
    }
}

