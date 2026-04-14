package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class GathererTest {

    @Test
    void getDiscount_returnsCorrectValue() {
        Gatherer g = new Gatherer("g", 3, 1, null);
        assertEquals(3, g.getDiscount());
    }

    @Test
    void getEra_returnsCorrectValue() {
        Gatherer g = new Gatherer("g", 3, 2, null);
        assertEquals(2, g.getEra());
    }

    @Test
    void getId_returnsCorrectId() {
        Gatherer g = new Gatherer("gatherer_01", 3, 1, null);
        assertEquals("gatherer_01", g.getId());
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Gatherer g = new Gatherer("g", 3, 1, null);
        Player p = new Player("Test");
        assertTrue(g.canBeDrawn(p));
    }

    @Test
    void insert_addsToGathererSet() {
        Gatherer g = new Gatherer("g", 3, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        g.insert(cards);
        assertTrue(cards.get(CardType.GATHERER).contains(g));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
        Gatherer g = new Gatherer("g", 3, 1, null);
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
