package org.example.server.model.game;
import org.example.server.model.Player;
import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Color;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        // Arrange - fresh player before each test
        player = new Player("Gianpaolo", 3, 0, Color.RED, new HashMap<>());
    }

    // --- getName() tests ---

    @Test
    void testGetName() {
        assertEquals("Gianpaolo", player.getName());
    }

    // --- getFood() tests ---

    @Test
    void testGetFood() {
        assertEquals(3, player.getFood());
    }

    // --- getPp() tests ---

    @Test
    void testGetPp() {
        assertEquals(0, player.getPp());
    }

    // --- getColor() tests ---

    @Test
    void testGetColor() {
        assertEquals(Color.RED, player.getColor());
    }

    // --- changePP() tests ---

    @Test
    void testChangePPIncrease() {
        player.changePP(5);
        assertEquals(5, player.getPp());
    }

    @Test
    void testChangePPDecrease() {
        player.changePP(-3);
        assertEquals(-3, player.getPp());
    }

    @Test
    void testChangePPMultipleTimes() {
        player.changePP(5);
        player.changePP(-2);
        assertEquals(3, player.getPp());
    }

    // --- changeFood() tests ---

    @Test
    void testChangeFoodIncrease() {
        player.changeFood(4);
        assertEquals(7, player.getFood());
    }

    @Test
    void testChangeFoodDecrease() {
        player.changeFood(-3);
        assertEquals(0, player.getFood());
    }

    // --- setLastPick() / getLastPick() tests ---

    @Test
    void testSetLastPick() {
        FakeCard card = new FakeCard();
        player.setLastPick(card);
        assertSame(card, player.getLastPick());
    }

    @Test
    void testLastPickDefaultIsNull() {
        Player fresh = new Player("Empty");
        assertNull(fresh.getLastPick());
    }

    // --- setColor() tests ---

    @Test
    void testSetColor() {
        player.setColor(Color.BLUE);
        assertEquals(Color.BLUE, player.getColor());
    }

    // --- single-arg constructor defaults ---

    @Test
    void testSingleArgConstructorDefaults() {
        Player fresh = new Player("Solo");
        assertAll(
                () -> assertEquals("Solo", fresh.getName()),
                () -> assertEquals(0, fresh.getFood()),
                () -> assertEquals(0, fresh.getPp()),
                () -> assertNull(fresh.getColor())
        );
    }
}