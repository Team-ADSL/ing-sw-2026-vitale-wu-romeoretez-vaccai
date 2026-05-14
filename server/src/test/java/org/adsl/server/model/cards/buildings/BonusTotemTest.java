package org.adsl.server.model.cards.buildings;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BonusTotemTest {

    private Player player;
    private BonusTotem bonusTotem;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
        bonusTotem = new BonusTotem("bt_01", 3, 2, 1, null);
    }

    @Test
    void testActiveEffect_endTurnTrigger_setsBonusFoodTileTrue() {
        bonusTotem.activeEffect(Set.of(player), Trigger.END_TURN);
        assertTrue(player.getBuildingBonus().isBonusFoodTile());
    }

    @Test
    void testActiveEffect_wrongTrigger_doesNotSetBonusFoodTile() {
        bonusTotem.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertFalse(player.getBuildingBonus().isBonusFoodTile());
    }

    @Test
    void testActiveEffect_endGameTrigger_doesNotSetBonusFoodTile() {
        bonusTotem.activeEffect(Set.of(player), Trigger.END_GAME);
        assertFalse(player.getBuildingBonus().isBonusFoodTile());
    }

    @Test
    void testActiveEffect_emptyPlayerSet_doesNotThrow() {
        assertDoesNotThrow(() -> bonusTotem.activeEffect(Set.of(), Trigger.END_TURN));
    }
}
