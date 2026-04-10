package org.example.server.model.cards.buildings.forEvent;

import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.characters.Hunter;
import org.example.server.model.cards.characters.Inventor;
import org.example.shared.enums.Icon;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class DuringSustenanceTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
    }

    @Test
    void getTypeMultiplier_returnsCorrectType() {
        DuringSustenance building = new DuringSustenance("ds", 3, 2, 1, null, CardType.HUNTER);
        assertEquals(CardType.HUNTER, building.getTypeMultiplier());
    }

    @Test
    void activeEffect_sustenanceTrigger_setsSustenanceDiscountToCardCount() {
        // Player has 3 hunters
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h3", false, 1, null));

        DuringSustenance building = new DuringSustenance("ds", 3, 2, 1, null, CardType.HUNTER);
        building.activeEffect(Set.of(player), Trigger.SUSTENANCE);

        assertEquals(3, player.getBuildingBonus().getSustenanceDiscount());
    }

    @Test
    void activeEffect_sustenanceTrigger_noCardsOfType_setsDiscountToZero() {
        DuringSustenance building = new DuringSustenance("ds", 3, 2, 1, null, CardType.INVENTOR);
        building.activeEffect(Set.of(player), Trigger.SUSTENANCE);
        assertEquals(0, player.getBuildingBonus().getSustenanceDiscount());
    }

    @Test
    void activeEffect_sustenanceTrigger_differentTypeMultiplier_countsCorrectType() {
        // 2 inventors, 1 hunter — typeMultiplier is INVENTOR
        player.getCards().get(CardType.INVENTOR).add(new Inventor("i1", Icon.BOAT, 1, null));
        player.getCards().get(CardType.INVENTOR).add(new Inventor("i2", Icon.ROPE, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h", false, 1, null));

        DuringSustenance building = new DuringSustenance("ds", 3, 2, 1, null, CardType.INVENTOR);
        building.activeEffect(Set.of(player), Trigger.SUSTENANCE);

        assertEquals(2, player.getBuildingBonus().getSustenanceDiscount());
    }

    @Test
    void activeEffect_wrongTrigger_doesNotSetDiscount() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h", false, 1, null));
        DuringSustenance building = new DuringSustenance("ds", 3, 2, 1, null, CardType.HUNTER);
        building.activeEffect(Set.of(player), Trigger.HUNT);
        assertEquals(0, player.getBuildingBonus().getSustenanceDiscount());
    }

    @Test
    void activeEffect_endGameTrigger_doesNotSetDiscount() {
        player.getCards().get(CardType.HUNTER).add(new Hunter("h", false, 1, null));
        DuringSustenance building = new DuringSustenance("ds", 3, 2, 1, null, CardType.HUNTER);
        building.activeEffect(Set.of(player), Trigger.END_GAME);
        assertEquals(0, player.getBuildingBonus().getSustenanceDiscount());
    }

    @Test
    void activeEffect_calledTwice_discountAccumulates() {
        // setSustenanceDiscount uses += internally, so two calls stack
        player.getCards().get(CardType.HUNTER).add(new Hunter("h1", false, 1, null));
        player.getCards().get(CardType.HUNTER).add(new Hunter("h2", false, 1, null));

        DuringSustenance building = new DuringSustenance("ds", 3, 2, 1, null, CardType.HUNTER);
        building.activeEffect(Set.of(player), Trigger.SUSTENANCE);
        building.activeEffect(Set.of(player), Trigger.SUSTENANCE);

        assertEquals(4, player.getBuildingBonus().getSustenanceDiscount());
    }

    @Test
    void activeEffect_emptyPlayerSet_doesNotThrow() {
        DuringSustenance building = new DuringSustenance("ds", 3, 2, 1, null, CardType.HUNTER);
        assertDoesNotThrow(() -> building.activeEffect(Set.of(), Trigger.SUSTENANCE));
    }
}