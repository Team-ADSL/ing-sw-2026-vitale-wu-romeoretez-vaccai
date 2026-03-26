package org.example.model.cards.buildings;

import org.example.server.model.cards.buildings.EndGame;
import org.example.server.model.cards.characters.Hunter;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.buildings.utils.BuildingEffect;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EndGameTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 10, 0, Color.RED);
    }

    @Test
    void endBuilderMultiplier_endGameTrigger_setsBuilderMultiplierTo2() {
        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.END_BUILDER_MULTIPLIER, null);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(2, player.getBuildingBonus().getBuilderMultiplierPP());
    }

    @Test
    void endBuilderMultiplier_wrongTrigger_doesNotChangeMultiplier() {
        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.END_BUILDER_MULTIPLIER, null);
        building.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(1, player.getBuildingBonus().getBuilderMultiplierPP()); // default is 1
    }

    @Test
    void ppCompleteSet_endGameTrigger_awardsMinCardCount_times6() {
        // Give the player 2 cards of each type => min = 2, so PP = 2 * 6 = 12
        for (CardType type : new CardType[]{CardType.HUNTER, CardType.GATHERER, CardType.SHAMAN,
                CardType.BUILDER, CardType.INVENTOR, CardType.ARTIST, CardType.BUILDINGS}) {
            player.getCards().get(type).add(new Hunter(false, 1, Optional.empty()));
            player.getCards().get(type).add(new Hunter(false, 1, Optional.empty()));
        }
        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.PP_COMPLETE_SET, null);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(12, player.getPp());
    }

    @Test
    void ppCompleteSet_someTypesEmpty_awardsZeroPP() {
        // No cards at all => min = 0, so PP = 0
        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.PP_COMPLETE_SET, null);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(0, player.getPp());
    }

    @Test
    void endCharacterMultiplier_endGameTrigger_awardsPPBasedOnCardTypeCount() {
        // Add 3 hunters => should award 3 PP
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));

        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.END_CHARACTER_MULTIPLIER, CardType.HUNTER);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(3, player.getPp());
    }

    @Test
    void endCharacterMultiplier_noCardsOfType_awardszero() {
        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.END_CHARACTER_MULTIPLIER, CardType.INVENTOR);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(0, player.getPp());
    }

    @Test
    void endPpBonus_endGameTrigger_awards25PP() {
        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.END_PP_BONUS, null);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(25, player.getPp());
    }

    @Test
    void endPpBonus_wrongTrigger_doesNotAwardPP() {
        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.END_PP_BONUS, null);
        building.activeEffect(Set.of(player), Trigger.END_TURN);
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        EndGame building = new EndGame(5, 3, 1, Optional.empty(), BuildingEffect.END_PP_BONUS, null);
        assertDoesNotThrow(() -> building.activeEffect(Set.of(), Trigger.END_GAME));
    }
}