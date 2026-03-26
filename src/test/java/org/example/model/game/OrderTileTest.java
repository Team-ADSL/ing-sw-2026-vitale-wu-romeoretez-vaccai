package org.example.model.game;
import org.example.server.model.Player;
import org.example.server.model.board.OrderCell;
import org.example.server.model.board.OrderTile;
import org.example.shared.enums.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class OrderTileTest {

    private OrderTile orderTile;
    private Player gianpaolo;
    private Player gianpiero;

    @BeforeEach
    void setUp() {
        // Arrange - create two empty cells and one tile
        gianpaolo = new Player("Gianpaolo", 3, 0, Color.RED);
        gianpiero = new Player("Gianpiero", 2, 0, Color.BLUE);

        ArrayList<OrderCell> cells = new ArrayList<>();
        cells.add(new OrderCell(Optional.empty(), 1, false));
        cells.add(new OrderCell(Optional.empty(), 0, false));
        cells.add(new OrderCell(Optional.empty(), 0, true));

        orderTile = new OrderTile(cells);
    }

    // --- getPlayerAt() tests ---

    @Test
    void testGetPlayerAtReturnsEmptyWhenNoPlayer() {
        // should return empty when no player has been placed
        assertTrue(orderTile.getPlayerAt(0).isEmpty());
    }

    @Test
    void testGetPlayerAtReturnsPlayerAfterPlacement() {
        // should return the player after being placed
        orderTile.placePlayerAtNext(gianpaolo);
        assertTrue(orderTile.getPlayerAt(0).isPresent());
        assertEquals(gianpaolo, orderTile.getPlayerAt(0).get());
    }

    @Test
    void testGetPlayerAtThrowsOnInvalidIndex() {
        // should throw when index is out of bounds
        assertThrows(IndexOutOfBoundsException.class, () -> orderTile.getPlayerAt(5));
    }

    // --- placePlayerAtNext() tests ---

    @Test
    void testPlacePlayerAtNextReturnsCorrectIndex() {
        // should return index 0 when all cells are empty
        int index = orderTile.placePlayerAtNext(gianpaolo);
        assertEquals(0, index);
    }

    @Test
    void testPlacePlayerAtNextFillsInOrder() {
        // should fill cells sequentially
        orderTile.placePlayerAtNext(gianpaolo);
        int index = orderTile.placePlayerAtNext(gianpiero);
        assertEquals(1, index);
    }

    @Test
    void testPlacePlayerAtNextAssignsCorrectPlayer() {
        // should assign the correct player to the correct cell
        orderTile.placePlayerAtNext(gianpaolo);
        orderTile.placePlayerAtNext(gianpiero);
        assertEquals(gianpaolo, orderTile.getPlayerAt(0).get());
        assertEquals(gianpiero, orderTile.getPlayerAt(1).get());
    }

    @Test
    void testPlacePlayerAtNextThrowsWhenFull() {
        // should throw when all cells are occupied
        orderTile.placePlayerAtNext(gianpaolo);
        orderTile.placePlayerAtNext(gianpiero);
        orderTile.placePlayerAtNext(new Player("Gianluca", 1, 0, Color.YELLOW));
        assertThrows(IndexOutOfBoundsException.class, () -> orderTile.placePlayerAtNext(new Player("Bowser", 1, 0, Color.WHITE)));
    }

    // --- getCellAt() tests ---

    @Test
    void testGetCellAtReturnsCorrectCell() {
        // should return the cell at the given index
        assertNotNull(orderTile.getCellAt(0));
    }

    @Test
    void testGetCellAtThrowsOnInvalidIndex() {
        // should throw when index is out of bounds
        assertThrows(IndexOutOfBoundsException.class, () -> orderTile.getCellAt(5));
    }
}
