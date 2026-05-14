package org.adsl.server.model.game;

import org.adsl.server.model.Player;
import org.adsl.server.model.board.OrderCell;
import org.adsl.server.model.board.OrderTile;
import org.adsl.shared.enums.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class OrderTileTest {

    private OrderTile orderTile;
    private Player gianpaolo;
    private Player gianpiero;

    @BeforeEach
    void setUp() {
        gianpaolo = new Player("Gianpaolo", 3, 0, Totem.RED, new java.util.HashMap<>());
        gianpiero = new Player("Gianpiero", 2, 0, Totem.BLUE, new java.util.HashMap<>());

        ArrayList<OrderCell> cells = new ArrayList<>();
        cells.add(new OrderCell(null, 1, false));
        cells.add(new OrderCell(null, 0, false));
        cells.add(new OrderCell(null, 0, true));

        orderTile = new OrderTile("order_tile_5p", cells);
    }

    // ──────────────────────────────────────────────
    // TEST GET PLAYER AT
    // ──────────────────────────────────────────────

    @Test
    void testGetPlayerAt_returnsEmptyWhenNoPlayer() {
        assertTrue(orderTile.getPlayerAt(0).isEmpty());
    }

    @Test
    void testGetPlayerAt_returnsPlayerAfterPlacement() {
        orderTile.placePlayerAtNext(gianpaolo);
        assertTrue(orderTile.getPlayerAt(0).isPresent());
        assertEquals(gianpaolo, orderTile.getPlayerAt(0).get());
    }

    @Test
    void testGetPlayerAt_throwsOnInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> orderTile.getPlayerAt(5));
    }

    // ──────────────────────────────────────────────
    // TEST PLACE PLAYER AT NEXT
    // ──────────────────────────────────────────────

    @Test
    void testPlacePlayerAtNext_returnsCorrectIndex() {
        int index = orderTile.placePlayerAtNext(gianpaolo);
        assertEquals(0, index);
    }

    @Test
    void testPlacePlayerAtNext_fillsInOrder() {
        orderTile.placePlayerAtNext(gianpaolo);
        int index = orderTile.placePlayerAtNext(gianpiero);
        assertEquals(1, index);
    }

    @Test
    void testPlacePlayerAtNext_assignsCorrectPlayer() {
        orderTile.placePlayerAtNext(gianpaolo);
        orderTile.placePlayerAtNext(gianpiero);
        assertEquals(gianpaolo, orderTile.getPlayerAt(0).get());
        assertEquals(gianpiero, orderTile.getPlayerAt(1).get());
    }

    @Test
    void testPlacePlayerAtNext_throwsWhenFull() {
        orderTile.placePlayerAtNext(gianpaolo);
        orderTile.placePlayerAtNext(gianpiero);
        orderTile.placePlayerAtNext(new Player("Gianluca", 1, 0, Totem.YELLOW, new java.util.HashMap<>()));
        assertThrows(IndexOutOfBoundsException.class, () -> orderTile.placePlayerAtNext(new Player("Bowser", 1, 0, Totem.WHITE, new java.util.HashMap<>())));
    }

    // ──────────────────────────────────────────────
    // TEST GET CELL AT
    // ──────────────────────────────────────────────

    @Test
    void testGetCellAt_returnsCorrectCell() {
        assertNotNull(orderTile.getCellAt(0));
    }

    @Test
    void testGetCellAt_throwsOnInvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> orderTile.getCellAt(5));
    }
}
