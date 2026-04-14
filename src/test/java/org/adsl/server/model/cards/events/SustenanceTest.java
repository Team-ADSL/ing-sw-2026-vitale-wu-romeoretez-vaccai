package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.characters.Gatherer;
import org.adsl.server.model.cards.characters.Hunter;
import org.adsl.server.model.cards.buildings.BonusTotem;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class SustenanceTest {

    private Player player;
    private Sustenance sustenance;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
        player.changeFood(10);
        sustenance = new Sustenance("sus_01", 2, false, 1, null);
    }

    @Test
    void getLostPP_returnsCorrectValue() {
        assertEquals(2, sustenance.getLostPP());
    }

    @Test
    void getId_returnsCorrectId() {
        assertEquals("sus_01", sustenance.getId());
    }

    @Test
    void canBeDrawn_alwaysFalse() {
        assertFalse(sustenance.canBeDrawn(player));
    }

    @Test
    void activeEffect_wrongTrigger_noEffect() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h", false, 1, null));
        sustenance.activeEffect(Set.of(player), Trigger.END_ROUND);
        assertEquals(10, player.getFood());
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_noCharacters_noFoodPaid() {
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(10, player.getFood());
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_threeCharacters_enoughFood_paysExactly() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h3", false, 1, null));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(7, player.getFood());
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_notEnoughFood_losesPP() {
        player = new Player("Poor");
        player.changeFood(1);
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h3", false, 1, null));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(0, player.getFood());
        assertEquals(-4, player.getPp());
    }

    @Test
    void activeEffect_noFoodAtAll_allCharactersUnfed_maxPPLoss() {
        player = new Player("Broke");
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h3", false, 1, null));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(-6, player.getPp());
    }

    @Test
    void activeEffect_gathererDiscount_reducesFood() {
        player.getCards().get(CardType.GATHERER).add(new Gatherer("g", 0, 1, null));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(10, player.getFood());
        assertEquals(0, player.getPp());
    }

    @Test
    void activeEffect_gathererDiscountPartial_reducesPayment() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h3", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h4", false, 1, null));
        player.getCards().get(CardType.GATHERER).add(new Gatherer("g", 0, 1, null));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(8, player.getFood());
    }

    @Test
    void activeEffect_buildingBonusDiscount_reducesPayment() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h3", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h4", false, 1, null));
        player.getBuildingBonus().setSustenanceDiscount(2);
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(8, player.getFood());
    }

    @Test
    void activeEffect_buildingsNotCounted_onlyCharacters() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h", false, 1, null));
        player.getCards().get(CardType.BUILDINGS).add(new BonusTotem("bt", 3, 2, 1, null));
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(9, player.getFood());
    }

    @Test
    void activeEffect_resetsBonus() {
        player.getBuildingBonus().setSustenanceDiscount(3);
        sustenance.activeEffect(Set.of(player), Trigger.EVENT_EXECUTION);
        assertEquals(0, player.getBuildingBonus().getSustenanceDiscount());
    }
}
