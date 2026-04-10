package org.example.server.model.game;
import org.example.server.model.Player;
import org.example.server.model.board.OrderCell;
import org.example.shared.enums.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OrderCellTest {

    private OrderCell cellWithPlayer;
    private OrderCell cellWithoutPlayer;

    @BeforeEach
    void setUp() {
        // Arrange - cell with player, bonus and malus
        cellWithPlayer = new OrderCell(
                new Player("Gianpaolo", 3, 0, Totem.RED, new java.util.HashMap<>()),
                2,
                false
        );

        // Arrange - empty cell with malus
        cellWithoutPlayer = new OrderCell(
                null,
                0,
                true
        );
    }

    // --- getPlayer() tests ---

    @Test
    void testGetPlayerReturnsPlayer() {
        // should return the player when present
        assertTrue(cellWithPlayer.getPlayer().isPresent());
    }

    @Test
    void testGetPlayerReturnsEmpty() {
        // should return empty when no player assigned
        assertTrue(cellWithoutPlayer.getPlayer().isEmpty());
    }

    // --- setPlayer() tests ---

    @Test
    void testSetPlayerAssignsPlayer() {
        // should assign a player to an empty cell
        Player newPlayer = new Player("Luigi", 2, 0, Totem.BLUE, new java.util.HashMap<>());
        cellWithoutPlayer.setPlayer(newPlayer);
        assertTrue(cellWithoutPlayer.getPlayer().isPresent());
    }

    @Test
    void testSetPlayerRemovesPlayer() {
        // should remove the player when set to empty
        cellWithPlayer.setPlayer(null);
        assertTrue(cellWithPlayer.getPlayer().isEmpty());
    }

    // --- getBonus() tests ---

    @Test
    void testGetBonusReturnsCorrectValue() {
        // should return the bonus passed in constructor
        assertEquals(2, cellWithPlayer.getBonus());
    }

    @Test
    void testGetBonusReturnsZero() {
        // should return 0 when no bonus
        assertEquals(0, cellWithoutPlayer.getBonus());
    }

    // --- isMalus() tests ---

    @Test
    void testIsMalusReturnsFalse() {
        // should return false when cell is not a malus
        assertFalse(cellWithPlayer.isMalus());
    }

    @Test
    void testIsMalusReturnsTrue() {
        // should return true when cell is a malus
        assertTrue(cellWithoutPlayer.isMalus());
    }
}
