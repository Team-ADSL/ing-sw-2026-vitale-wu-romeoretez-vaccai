package org.adsl.server.model.cards.buildings;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.characters.Builder;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BuildingTest {

    private static class ConcreteBuilding extends Building {
        public ConcreteBuilding(String id, int endGamePP, int cost, int era, Integer numPlayers) {
            super(id, endGamePP, cost, era, numPlayers);
        }

        @Override
        public void activeEffect(Set<Player> players, Trigger t) {}
    }

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester");
        player.changeFood(10);
    }

    @Test
    void getCost_returnsCorrectValue() {
        Building b = new ConcreteBuilding("cb", 5, 3, 1, null);
        assertEquals(3, b.getCost());
    }

    @Test
    void getId_returnsCorrectId() {
        Building b = new ConcreteBuilding("building_01", 5, 3, 1, null);
        assertEquals("building_01", b.getId());
    }

    @Test
    void canBeDrawn_nullPlayer_returnsFalse() {
        Building b = new ConcreteBuilding("cb", 5, 3, 1, null);
        assertFalse(b.canBeDrawn(null));
    }

    @Test
    void canBeDrawn_insufficientFood_returnsFalse() {
        Player poor = new Player("Poor");
        poor.changeFood(2);
        Building b = new ConcreteBuilding("cb", 5, 3, 1, null);
        assertFalse(b.canBeDrawn(poor));
    }

    @Test
    void canBeDrawn_exactFood_returnsTrue() {
        Player exact = new Player("Exact");
        exact.changeFood(3);
        Building b = new ConcreteBuilding("cb", 5, 3, 1, null);
        assertTrue(b.canBeDrawn(exact));
    }

    @Test
    void canBeDrawn_moreThanEnoughFood_returnsTrue() {
        Building b = new ConcreteBuilding("cb", 5, 3, 1, null);
        assertTrue(b.canBeDrawn(player));
    }

    @Test
    void canBeDrawn_withBuilderDiscount_makesAffordable() {
        Player p = new Player("Discounted");
        p.changeFood(1);
        Builder builder = new Builder("b", 2, 0, 1, null);
        p.getCards().get(CardType.BUILDER).add(builder);
        Building b = new ConcreteBuilding("cb", 5, 3, 1, null);
        assertTrue(b.canBeDrawn(p));
    }

    @Test
    void canBeDrawn_withMultipleBuilderDiscounts_sumsCorrectly() {
        Player p = new Player("MultiDiscount");
        p.getCards().get(CardType.BUILDER).add(new Builder("b1", 2, 0, 1, null));
        p.getCards().get(CardType.BUILDER).add(new Builder("b2", 2, 0, 1, null));
        Building b = new ConcreteBuilding("cb", 5, 3, 1, null);
        assertTrue(b.canBeDrawn(p));
    }

    @Test
    void insert_addsBuildingToCardsMap() {
        Building b = new ConcreteBuilding("cb", 5, 3, 1, null);
        Map<CardType, Set<Card>> cards = new HashMap<>();
        cards.put(CardType.BUILDINGS, new HashSet<>());
        b.insert(cards);
        assertTrue(cards.get(CardType.BUILDINGS).contains(b));
    }
}
