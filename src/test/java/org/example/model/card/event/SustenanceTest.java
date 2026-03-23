package org.example.model.card.event;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.BonusTotem;
import org.example.model.card.character.*;
import org.example.model.game.Color;
import org.example.model.game.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SustenanceTest {

    private Player player;
    // lostPP = 2 per unfed character
    private Sustenance sustenance;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 10, 0, Color.RED);
        sustenance = new Sustenance(2, false, 1, Optional.empty());
    }

    @Test
    void getLostPP_returnsCorrectValue() {
        assertEquals(2, sustenance.getLostPP());
    }

    @Test
    void canBeDrawn_alwaysFalse() {
        assertFalse(sustenance.canBeDrawn(player));
    }

    @Test
    void activeEffect_wrongTrigger_noEffect() {
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        sustenance.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(10, player.getFood());
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_noCharacters_noFoodPaid() {
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(10, player.getFood()); // unchanged
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_threeCharacters_enoughFood_paysExactly() {
        // 3 chars, 10 food, discount=0 → pays 3
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(7, player.getFood()); // 10 - 3
        assertEquals(0, player.getPp());   // no unfed chars
    }

    @Test
    void activeEffect_notEnoughFood_losesPP() {
        // 3 chars, 1 food, discount=0 → foodToPay=3, pays 1, 2 unfed → PP -= 4
        player = new Player("Poor", 1, 0, Color.RED);
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(0, player.getFood());
        assertEquals(-4, player.getPp()); // 2 unfed * lostPP(2)
    }

    @Test
    void activeEffect_noFoodAtAll_allCharactersUnfed_maxPPLoss() {
        // 3 chars, 0 food → 3 unfed → PP -= 6
        player = new Player("Broke", 0, 0, Color.RED);
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(-6, player.getPp());
    }

    @Test
    void activeEffect_gathererDiscount_reducesFood() {
        // 1 gatherer (= 1 char), discount = 1*3 = 3 → foodToPay = max(0, 1-3) = 0
        player.getCards().get(CardType.GATHERER).add(new Gatherer(0, 1, Optional.empty()));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(10, player.getFood()); // no food paid
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_gathererDiscountPartial_reducesPayment() {
        // 4 hunters + 1 gatherer = 5 chars, discount = 3 → foodToPay = 2
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.GATHERER).add(new Gatherer(0, 1, Optional.empty()));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(8, player.getFood()); // 10 - 2
    }

    @Test
    void activeEffect_buildingBonusDiscount_reducesPayment() {
        // 4 chars, sustenanceDiscount=2 → foodToPay = 4 - 2 = 2
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        player.getBuildingBonus().setSustenanceDiscount(2);
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(8, player.getFood()); // 10 - 2
    }

    @Test
    void activeEffect_buildingsNotCounted_onlyCharacters() {
        // Buildings should NOT count towards totalCharacters
        player.getCards().get(CardType.HUNTER).add(new Hunter(false, 1, Optional.empty()));
        // add a real Building to the BUILDINGS set
        player.getCards().get(CardType.BUILDINGS).add(new BonusTotem(3, 2, 1, Optional.empty()));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(9, player.getFood()); // pays 1 for the hunter only, not the building
    }

    @Test
    void activeEffect_resetsBonus() {
        player.getBuildingBonus().setSustenanceDiscount(3);
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(0, player.getBuildingBonus().getSustenanceDiscount());
    }
}
