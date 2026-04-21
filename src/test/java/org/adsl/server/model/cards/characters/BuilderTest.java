package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BuilderTest {

    @Test
    void testCanBeDrawn_alwaysTrue() {
        Builder b = new Builder("b", 2, 5, 1, null);
        Player p = new Player("Test");
        assertTrue(b.canBeDrawn(p));
    }

    @Test
    void testInsert_addsToBuilderSet() {
        Builder b = new Builder("b", 2, 5, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        b.insert(cards);
        assertTrue(cards.get(CardType.BUILDER).contains(b));
    }

    @Test
    void testInsert_doesNotAddToOtherSets() {
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
