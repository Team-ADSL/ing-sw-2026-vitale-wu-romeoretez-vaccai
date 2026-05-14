package org.adsl.server.model.cards.buildings.forEvent;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DuringHuntTest {

    private Player player;
    private DuringHunt duringHunt;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
        duringHunt = new DuringHunt("dh_01", 3, 2, 1, null);
    }

    @Test
    void testActiveEffect_huntTrigger_setsHuntEventBonusTrue() {
        duringHunt.activeEffect(Set.of(player), Trigger.HUNT);
        assertTrue(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void testActiveEffect_wrongTrigger_doesNotSetHuntBonus() {
        duringHunt.activeEffect(Set.of(player), Trigger.SUSTENANCE);
        assertFalse(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void testActiveEffect_endGameTrigger_doesNotSetHuntBonus() {
        duringHunt.activeEffect(Set.of(player), Trigger.END_GAME);
        assertFalse(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void testActiveEffect_shamanicRitual_doesNotSetHuntBonus() {
        duringHunt.activeEffect(Set.of(player), Trigger.SHAMANIC_RITUAL);
        assertFalse(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void testActiveEffect_emptyPlayerSet_doesNotThrow() {
        assertDoesNotThrow(() -> duringHunt.activeEffect(Set.of(), Trigger.HUNT));
    }
}
