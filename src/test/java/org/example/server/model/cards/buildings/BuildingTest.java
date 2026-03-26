package org.example.server.model.cards.buildings;

import org.example.server.model.cards.Card;
import org.example.server.model.cards.buildings.Building;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.cards.characters.Builder;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BuildingTest {

    // Concrete subclass to test the abstract Building
    private static class ConcreteBuilding extends Building {
        public ConcreteBuilding(int endGamePP, int cost, int era, Optional<Integer> numPlayers) {
            super(endGamePP, cost, era, numPlayers);
        }

        @Override
        public void activeEffect(Set<Player> players, Trigger t) {}
    }

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("Tester", 10, 0, Color.RED);
    }

    @Test
    void getCost_returnsCorrectValue() {
        Building b = new ConcreteBuilding(5, 3, 1, Optional.empty());
        assertEquals(3, b.getCost());
    }

    @Test
    void canBeDrawn_nullPlayer_returnsFalse() {
        Building b = new ConcreteBuilding(5, 3, 1, Optional.empty());
        assertFalse(b.canBeDrawn(null));
    }

    @Test
    void canBeDrawn_insufficientFood_returnsFalse() {
        Player poor = new Player("Poor", 2, 0, Color.RED);
        Building b = new ConcreteBuilding(5, 3, 1, Optional.empty());
        assertFalse(b.canBeDrawn(poor));
    }

    @Test
    void canBeDrawn_exactFood_returnsTrue() {
        Player exact = new Player("Exact", 3, 0, Color.RED);
        Building b = new ConcreteBuilding(5, 3, 1, Optional.empty());
        assertTrue(b.canBeDrawn(exact));
    }

    @Test
    void canBeDrawn_moreThanEnoughFood_returnsTrue() {
        Building b = new ConcreteBuilding(5, 3, 1, Optional.empty());
        assertTrue(b.canBeDrawn(player)); // player has 10 food, cost is 3
    }

    @Test
    void canBeDrawn_withBuilderDiscount_makesAffordable() {
        // Player has 1 food, cost is 3, builder gives discount 2 => effective cost 1
        Player p = new Player("Discounted", 1, 0, Color.RED);
        Builder builder = new Builder(2, 0, 1, Optional.empty());
        p.getCards().get(CardType.BUILDER).add(builder);
        Building b = new ConcreteBuilding(5, 3, 1, Optional.empty());
        assertTrue(b.canBeDrawn(p));
    }

    @Test
    void canBeDrawn_withMultipleBuilderDiscounts_sumsCorrectly() {
        // Player has 0 food, cost is 3, two builders each give 2 discount => total 4 >= 3
        Player p = new Player("MultiDiscount", 0, 0, Color.RED);
        p.getCards().get(CardType.BUILDER).add(new Builder(2, 0, 1, Optional.empty()));
        p.getCards().get(CardType.BUILDER).add(new Builder(2, 0, 1, Optional.empty()));
        Building b = new ConcreteBuilding(5, 3, 1, Optional.empty());
        assertTrue(b.canBeDrawn(p));
    }

    @Test
    void insert_addsBuildingToCardsMap() {
        Building b = new ConcreteBuilding(5, 3, 1, Optional.empty());
        Map<CardType, Set<Card>> cards = new HashMap<>();
        cards.put(CardType.BUILDINGS, new HashSet<>());
        b.insert(cards);
        assertTrue(cards.get(CardType.BUILDINGS).contains(b));
    }
}