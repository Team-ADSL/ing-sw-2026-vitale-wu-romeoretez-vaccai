package org.adsl.server.model.game;
import org.adsl.server.model.Player;
import org.adsl.shared.enums.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Gianpaolo", 3, 0, Totem.RED, new HashMap<>());
    }

    // ──────────────────────────────────────────────
    // TEST CHANGE PP
    // ──────────────────────────────────────────────

    @Test
    void testChangePP_increase() {
        player.changePP(5);
        assertEquals(5, player.getPp());
    }

    @Test
    void testChangePP_decrease() {
        player.changePP(-3);
        assertEquals(-3, player.getPp());
    }

    @Test
    void testChangePP_multipleTimes() {
        player.changePP(5);
        player.changePP(-2);
        assertEquals(3, player.getPp());
    }

    // ──────────────────────────────────────────────
    // TEST CHANGE FOOD
    // ──────────────────────────────────────────────

    @Test
    void testChangeFood_increase() {
        player.changeFood(4);
        assertEquals(7, player.getFood());
    }

    @Test
    void testChangeFood_decrease() {
        player.changeFood(-3);
        assertEquals(0, player.getFood());
    }

    // ──────────────────────────────────────────────
    // TEST SET LAST PICK
    // ──────────────────────────────────────────────

    @Test
    void testSetLastPick_storesCard() {
        FakeCard card = new FakeCard();
        player.setLastPick(card);
        assertSame(card, player.getLastPick());
    }

    @Test
    void testLastPick_defaultIsNull() {
        Player fresh = new Player("Empty");
        assertNull(fresh.getLastPick());
    }

    // ──────────────────────────────────────────────
    // TEST SET COLOR
    // ──────────────────────────────────────────────

    @Test
    void testSetColor_updatesColor() {
        player.setColor(Totem.BLUE);
        assertEquals(Totem.BLUE, player.getColor());
    }

    // ──────────────────────────────────────────────
    // TEST SINGLE-ARG CONSTRUCTOR DEFAULTS
    // ──────────────────────────────────────────────

    @Test
    void testSingleArgConstructor_defaultValues() {
        Player fresh = new Player("Solo");
        assertAll(
                () -> assertEquals("Solo", fresh.getName()),
                () -> assertEquals(0, fresh.getFood()),
                () -> assertEquals(0, fresh.getPp()),
                () -> assertNull(fresh.getColor())
        );
    }
}
