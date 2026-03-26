package org.example.server.model.cards.buildings.forEvent;

import org.example.server.model.cards.buildings.forEvent.DuringHunt;
import org.example.shared.enums.Trigger;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DuringHuntTest {

    private Player player;
    private DuringHunt duringHunt;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 10, 0, Color.RED);
        duringHunt = new DuringHunt(3, 2, 1, Optional.empty());
    }

    @Test
    void activeEffect_huntTrigger_setsHuntEventBonusTrue() {
        duringHunt.activeEffect(Set.of(player), Trigger.HUNT);
        assertTrue(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void activeEffect_wrongTrigger_doesNotSetHuntBonus() {
        duringHunt.activeEffect(Set.of(player), Trigger.SUSTENANCE);
        assertFalse(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void activeEffect_endGameTrigger_doesNotSetHuntBonus() {
        duringHunt.activeEffect(Set.of(player), Trigger.END_GAME);
        assertFalse(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void activeEffect_shamanicritual_doesNotSetHuntBonus() {
        duringHunt.activeEffect(Set.of(player), Trigger.SHAMANIC_RITUAL);
        assertFalse(player.getBuildingBonus().isHuntEventBonus());
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        assertDoesNotThrow(() -> duringHunt.activeEffect(Set.of(), Trigger.HUNT));
    }
}
