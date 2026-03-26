package org.example.model.cards.buildings.forEvent;

import org.example.server.model.cards.buildings.forEvent.DuringPainting;
import org.example.shared.enums.Trigger;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DuringPaintingTest {

    private Player player;
    private DuringPainting duringPainting;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 10, 0, Color.RED);
        duringPainting = new DuringPainting(3, 2, 1, Optional.empty());
    }

    @Test
    void activeEffect_cavePaintingsTrigger_setsArtistFoodTrue() {
        duringPainting.activeEffect(Set.of(player), Trigger.CAVE_PAINTINGS);
        assertTrue(player.getBuildingBonus().isArtistFood());
    }

    @Test
    void activeEffect_wrongTrigger_doesNotSetArtistFood() {
        duringPainting.activeEffect(Set.of(player), Trigger.HUNT);
        assertFalse(player.getBuildingBonus().isArtistFood());
    }

    @Test
    void activeEffect_sustenanceTrigger_doesNotSetArtistFood() {
        duringPainting.activeEffect(Set.of(player), Trigger.SUSTENANCE);
        assertFalse(player.getBuildingBonus().isArtistFood());
    }

    @Test
    void activeEffect_endGameTrigger_doesNotSetArtistFood() {
        duringPainting.activeEffect(Set.of(player), Trigger.END_GAME);
        assertFalse(player.getBuildingBonus().isArtistFood());
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        assertDoesNotThrow(() -> duringPainting.activeEffect(Set.of(), Trigger.CAVE_PAINTINGS));
    }
}
