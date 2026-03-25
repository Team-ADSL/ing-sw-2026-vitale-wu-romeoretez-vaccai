package org.example.model.game;
import org.example.model.Player;
import org.example.shared.enums.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        // Arrange - fresh player before each test
        player = new Player("Gianpaolo", 3, 0, Color.RED);
    }

    // --- getName() tests ---

    @Test
    void testGetName() {
        // should return the name passed in constructor
        assertEquals("Gianpaolo", player.getName());
    }

    // --- getFood() tests ---

    @Test
    void testGetFood() {
        // should return the food passed in constructor
        assertEquals(3, player.getFood());
    }

    // --- getPp() tests ---

    @Test
    void testGetPp() {
        // should return the pp passed in constructor
        assertEquals(0, player.getPp());
    }

    // --- getColor() tests ---

    @Test
    void testGetColor() {
        // should return the color passed in constructor
        assertEquals(Color.RED, player.getColor());
    }

    // --- changePP() tests ---

    @Test
    void testChangePPIncrease() {
        // pp should increase by the given amount
        player.changePP(5);
    }

}