package org.adsl.server.model.cards.buildings;

import org.adsl.server.model.cards.characters.Hunter;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingEffect;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EndGameTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
    }

    @Test
    void testEndBuilderMultiplier_endGameTrigger_setsBuilderMultiplierTo2() {
        EndGame building = new EndGame("eg", 5, 3, 1, null, BuildingEffect.END_BUILDER_MULTIPLIER);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(2, player.getBuildingBonus().getBuilderMultiplierPP());
    }

    @Test
    void testEndBuilderMultiplier_wrongTrigger_doesNotChangeMultiplier() {
        EndGame building = new EndGame("eg", 5, 3, 1, null, BuildingEffect.END_BUILDER_MULTIPLIER);
        building.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(1, player.getBuildingBonus().getBuilderMultiplierPP());
    }

    @Test
    void testPpCompleteSet_endGameTrigger_awardsMinCardCountTimes6() {
        for (CardType type : new CardType[]{CardType.HUNTER, CardType.GATHERER, CardType.SHAMAN,
                CardType.BUILDER, CardType.INVENTOR, CardType.ARTIST, CardType.BUILDINGS}) {
            player.getCards().get(type).add(new Hunter("h1", false, 1, null));
            player.getCards().get(type).add(new Hunter("h2", false, 1, null));
        }
        EndGame building = new EndGame("eg", 5, 3, 1, null, BuildingEffect.PP_COMPLETE_SET);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(12, player.getPp());
    }

    @Test
    void testPpCompleteSet_someTypesEmpty_awardsZeroPP() {
        EndGame building = new EndGame("eg", 5, 3, 1, null, BuildingEffect.PP_COMPLETE_SET);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(0, player.getPp());
    }

    @Test
    void testEndPpBonus_endGameTrigger_awards25PP() {
        EndGame building = new EndGame("eg", 5, 3, 1, null, BuildingEffect.END_PP_BONUS);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(25, player.getPp());
    }

    @Test
    void testEndPpBonus_wrongTrigger_doesNotAwardPP() {
        EndGame building = new EndGame("eg", 5, 3, 1, null, BuildingEffect.END_PP_BONUS);
        building.activeEffect(Set.of(player), Trigger.END_TURN);
        assertEquals(0, player.getPp());
    }

    @Test
    void testActiveEffect_emptyPlayerSet_doesNotThrow() {
        EndGame building = new EndGame("eg", 5, 3, 1, null, BuildingEffect.END_PP_BONUS);
        assertDoesNotThrow(() -> building.activeEffect(Set.of(), Trigger.END_GAME));
    }
}
