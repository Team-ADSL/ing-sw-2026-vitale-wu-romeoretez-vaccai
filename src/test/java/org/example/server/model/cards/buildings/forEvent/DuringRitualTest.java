package org.example.server.model.cards.buildings.forEvent;

import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.utils.BuildingEffect;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DuringRitualTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
    }

    // ---- RITUAL_IMMUNITY ----

    @Test
    void ritualImmunity_shamanicRitualTrigger_setsNoRitualLostPPTrue() {
        DuringRitual ritual = new DuringRitual("dr", 3, 2, 1, null, BuildingEffect.RITUAL_IMMUNITY);
        ritual.activeEffect(Set.of(player), Trigger.SHAMANIC_RITUAL);
        assertTrue(player.getBuildingBonus().isNoRitualLostPP());
    }

    @Test
    void ritualImmunity_wrongTrigger_doesNotSetImmunity() {
        DuringRitual ritual = new DuringRitual("dr", 3, 2, 1, null, BuildingEffect.RITUAL_IMMUNITY);
        ritual.activeEffect(Set.of(player), Trigger.HUNT);
        assertFalse(player.getBuildingBonus().isNoRitualLostPP());
    }

    // ---- RITUAL_STARS_BONUS ----

    @Test
    void ritualStarsBonus_shamanicRitualTrigger_setsExtraStarsTo3() {
        DuringRitual ritual = new DuringRitual("dr", 3, 2, 1, null, BuildingEffect.RITUAL_STARS_BONUS);
        ritual.activeEffect(Set.of(player), Trigger.SHAMANIC_RITUAL);
        assertEquals(3, player.getBuildingBonus().getExtraStars());
    }

    @Test
    void ritualStarsBonus_wrongTrigger_doesNotSetExtraStars() {
        DuringRitual ritual = new DuringRitual("dr", 3, 2, 1, null, BuildingEffect.RITUAL_STARS_BONUS);
        ritual.activeEffect(Set.of(player), Trigger.SUSTENANCE);
        assertEquals(0, player.getBuildingBonus().getExtraStars());
    }

    // ---- RITUAL_DOUBLE_PP ----

    @Test
    void ritualDoublePP_shamanicRitualTrigger_setsShamanMultiplierTo2() {
        DuringRitual ritual = new DuringRitual("dr", 3, 2, 1, null, BuildingEffect.RITUAL_DOUBLE_PP);
        ritual.activeEffect(Set.of(player), Trigger.SHAMANIC_RITUAL);
        assertEquals(2, player.getBuildingBonus().getShamanMulitiplierPP());
    }

    @Test
    void ritualDoublePP_wrongTrigger_doesNotChangeMultiplier() {
        DuringRitual ritual = new DuringRitual("dr", 3, 2, 1, null, BuildingEffect.RITUAL_DOUBLE_PP);
        ritual.activeEffect(Set.of(player), Trigger.CAVE_PAINTINGS);
        assertEquals(1, player.getBuildingBonus().getShamanMulitiplierPP()); // default is 1
    }

    // ---- Non-event triggers filtered by DuringEvent base class ----

    @Test
    void activeEffect_endRoundTrigger_neverFiresForAnyEffect() {
        DuringRitual ritual = new DuringRitual("dr", 3, 2, 1, null, BuildingEffect.RITUAL_DOUBLE_PP);
        ritual.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(1, player.getBuildingBonus().getShamanMulitiplierPP()); // unchanged
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        DuringRitual ritual = new DuringRitual("dr", 3, 2, 1, null, BuildingEffect.RITUAL_IMMUNITY);
        assertDoesNotThrow(() -> ritual.activeEffect(Set.of(), Trigger.SHAMANIC_RITUAL));
    }
}
