package org.example.server.model.cards.buildings;

import org.example.server.model.cards.buildings.BonusTotem;
import org.example.shared.enums.Trigger;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BonusTotemTest {

    private Player player;
    private BonusTotem bonusTotem;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 10, 0, Color.RED);
        bonusTotem = new BonusTotem(3, 2, 1, Optional.empty());
    }

    @Test
    void activeEffect_endTurnTrigger_setsBonusFoodTileTrue() {
        bonusTotem.activeEffect(Set.of(player), Trigger.END_TURN);
        assertTrue(player.getBuildingBonus().isBonusFoodTile());
    }

    @Test
    void activeEffect_wrongTrigger_doesNotSetBonusFoodTile() {
        bonusTotem.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertFalse(player.getBuildingBonus().isBonusFoodTile());
    }

    @Test
    void activeEffect_endGameTrigger_doesNotSetBonusFoodTile() {
        bonusTotem.activeEffect(Set.of(player), Trigger.END_GAME);
        assertFalse(player.getBuildingBonus().isBonusFoodTile());
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        assertDoesNotThrow(() -> bonusTotem.activeEffect(Set.of(), Trigger.END_TURN));
    }
}