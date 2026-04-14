package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BuilderTest {

    @Test
    void getDiscount_returnsCorrectValue() {
        Builder b = new Builder("b", 2, 5, 1, null);
        assertEquals(2, b.getDiscount());
    }

    @Test
    void getPP_returnsCorrectValue() {
        Builder b = new Builder("b", 2, 5, 1, null);
        assertEquals(5, b.getPP());
    }

    @Test
    void getEra_returnsCorrectValue() {
        Builder b = new Builder("b", 2, 5, 3, null);
        assertEquals(3, b.getEra());
    }

    @Test
    void getId_returnsCorrectId() {
        Builder b = new Builder("builder_01", 2, 5, 1, null);
        assertEquals("builder_01", b.getId());
    }

    @Test
    void getNumPlayers_returnsCorrectValue() {
        Builder b = new Builder("b", 2, 5, 1, 4);
        assertEquals(Optional.of(4), b.getNumPlayers());
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Builder b = new Builder("b", 2, 5, 1, null);
        Player p = new Player("Test");
        assertTrue(b.canBeDrawn(p));
    }

    @Test
    void insert_addsToBuilderSet() {
        Builder b = new Builder("b", 2, 5, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        b.insert(cards);
        assertTrue(cards.get(CardType.BUILDER).contains(b));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
        Builder b = new Builder("b", 2, 5, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        b.insert(cards);
        for (CardType type : CardType.values()) {
            if (type != CardType.BUILDER) {
                assertFalse(cards.get(type).contains(b));
            }
        }
    }

    private Map<CardType, Set<Card>> initCardsMap() {
        Map<CardType, Set<Card>> cards = new HashMap<>();
        for (CardType t : CardType.values()) cards.put(t, new HashSet<>());
        return cards;
    }
}
