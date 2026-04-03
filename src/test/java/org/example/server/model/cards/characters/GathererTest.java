package org.example.server.model.cards.characters;

import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class GathererTest {

    @Test
    void getDiscount_returnsCorrectValue() {
        Gatherer g = new Gatherer("g", 3, 1, Optional.empty());
        assertEquals(3, g.getDiscount());
    }

    @Test
    void getEra_returnsCorrectValue() {
        Gatherer g = new Gatherer("g", 3, 2, Optional.empty());
        assertEquals(2, g.getEra());
    }

    @Test
    void getId_returnsCorrectId() {
        Gatherer g = new Gatherer("gatherer_01", 3, 1, Optional.empty());
        assertEquals("gatherer_01", g.getId());
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Gatherer g = new Gatherer("g", 3, 1, Optional.empty());
        Player p = new Player("Test");
        assertTrue(g.canBeDrawn(p));
    }

    @Test
    void insert_addsToGathererSet() {
        Gatherer g = new Gatherer("g", 3, 1, Optional.empty());
        Map<CardType, Set<Card>> cards = initCardsMap();
        g.insert(cards);
        assertTrue(cards.get(CardType.GATHERER).contains(g));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
        Gatherer g = new Gatherer("g", 3, 1, Optional.empty());
        Map<CardType, Set<Card>> cards = initCardsMap();
        g.insert(cards);
        for (CardType type : CardType.values()) {
            if (type != CardType.GATHERER) {
                assertFalse(cards.get(type).contains(g));
            }
        }
    }

    private Map<CardType, Set<Card>> initCardsMap() {
        Map<CardType, Set<Card>> cards = new HashMap<>();
        for (CardType t : CardType.values()) cards.put(t, new HashSet<>());
        return cards;
    }
}
