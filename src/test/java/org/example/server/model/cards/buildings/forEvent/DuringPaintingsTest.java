package org.example.server.model.cards.buildings.forEvent;

import org.example.shared.enums.Trigger;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DuringPaintingsTest {

    private Player player;
    private DuringPaintings duringPaintings;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
        duringPaintings = new DuringPaintings("dp_01", 3, 2, 1, null);
    }

    @Test
    void getId_returnsCorrectId() {
        assertEquals("dp_01", duringPaintings.getId());
    }

    @Test
    void activeEffect_cavePaintingsTrigger_setsArtistFoodTrue() {
        duringPaintings.activeEffect(Set.of(player), Trigger.CAVE_PAINTINGS);
        assertTrue(player.getBuildingBonus().isArtistFood());
    }

    @Test
    void activeEffect_wrongTrigger_doesNotSetArtistFood() {
        duringPaintings.activeEffect(Set.of(player), Trigger.HUNT);
        assertFalse(player.getBuildingBonus().isArtistFood());
    }

    @Test
    void activeEffect_sustenanceTrigger_doesNotSetArtistFood() {
        duringPaintings.activeEffect(Set.of(player), Trigger.SUSTENANCE);
        assertFalse(player.getBuildingBonus().isArtistFood());
    }

    @Test
    void activeEffect_endGameTrigger_doesNotSetArtistFood() {
        duringPaintings.activeEffect(Set.of(player), Trigger.END_GAME);
        assertFalse(player.getBuildingBonus().isArtistFood());
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        assertDoesNotThrow(() -> duringPaintings.activeEffect(Set.of(), Trigger.CAVE_PAINTINGS));
    }
}
