package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ShamanTest {

    @Test
    void testCanBeDrawn_alwaysTrue() {
        Shaman s = new Shaman("s", 3, 1, null);
        Player p = new Player("Test");
        assertTrue(s.canBeDrawn(p));
    }

    @Test
    void testInsert_addsToShamanSet() {
        Shaman s = new Shaman("s", 3, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        s.insert(cards);
        assertTrue(cards.get(CardType.SHAMAN).contains(s));
    }

    @Test
    void testInsert_doesNotAddToOtherSets() {
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
