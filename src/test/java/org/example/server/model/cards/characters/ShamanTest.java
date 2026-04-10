package org.example.server.model.cards.characters;

import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ShamanTest {

    @Test
    void getStarNum_returnsCorrectValue() {
        Shaman s = new Shaman("s", 3, 1, null);
        assertEquals(3, s.getStarNum());
    }

    @Test
    void getEra_returnsCorrectValue() {
        Shaman s = new Shaman("s", 3, 2, null);
        assertEquals(2, s.getEra());
    }

    @Test
    void getId_returnsCorrectId() {
        Shaman s = new Shaman("shaman_01", 3, 1, null);
        assertEquals("shaman_01", s.getId());
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Shaman s = new Shaman("s", 3, 1, null);
        Player p = new Player("Test");
        assertTrue(s.canBeDrawn(p));
    }

    @Test
    void insert_addsToShamanSet() {
        Shaman s = new Shaman("s", 3, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        s.insert(cards);
        assertTrue(cards.get(CardType.SHAMAN).contains(s));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
        Shaman s = new Shaman("s", 3, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        s.insert(cards);
        for (CardType type : CardType.values()) {
            if (type != CardType.SHAMAN) {
                assertFalse(cards.get(type).contains(s));
            }
        }
    }

    private Map<CardType, Set<Card>> initCardsMap() {
        Map<CardType, Set<Card>> cards = new HashMap<>();
        for (CardType t : CardType.values()) cards.put(t, new HashSet<>());
        return cards;
    }
}
