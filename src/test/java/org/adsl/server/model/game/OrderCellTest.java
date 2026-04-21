package org.adsl.server.model.game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.OrderCell;
import org.adsl.shared.enums.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderCellTest {

    private OrderCell cellWithPlayer;
    private OrderCell cellWithoutPlayer;

    @BeforeEach
    void setUp() {
        cellWithPlayer = new OrderCell(
                new Player("Gianpaolo", 3, 0, Totem.RED, new java.util.HashMap<>()),
                2,
                false
        );

        cellWithoutPlayer = new OrderCell(
                null,
                0,
                true
        );
    }

    // ──────────────────────────────────────────────
    // TEST GET PLAYER
    // ──────────────────────────────────────────────

    @Test
    void testGetPlayer_returnsPlayer() {
        assertTrue(cellWithPlayer.getPlayer().isPresent());
    }

    @Test
    void testGetPlayer_returnsEmpty() {
        assertTrue(cellWithoutPlayer.getPlayer().isEmpty());
    }

    // ──────────────────────────────────────────────
    // TEST SET PLAYER
    // ──────────────────────────────────────────────

    @Test
    void testSetPlayer_assignsPlayer() {
        Player newPlayer = new Player("Luigi", 2, 0, Totem.BLUE, new java.util.HashMap<>());
        cellWithoutPlayer.setPlayer(newPlayer);
        assertTrue(cellWithoutPlayer.getPlayer().isPresent());
    }

    @Test
    void testSetPlayer_removesPlayer() {
        cellWithPlayer.setPlayer(null);
        assertTrue(cellWithPlayer.getPlayer().isEmpty());
    }

    // ──────────────────────────────────────────────
    // TEST IS MALUS
    // ──────────────────────────────────────────────

    @Test
    void testIsMalus_returnsFalse() {
        assertFalse(cellWithPlayer.isMalus());
    }

    @Test
    void testIsMalus_returnsTrue() {
        assertTrue(cellWithoutPlayer.isMalus());
    }
}
