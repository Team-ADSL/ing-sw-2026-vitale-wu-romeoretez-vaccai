package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.Player;
import org.adsl.shared.enums.Icon;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class InventorTest {

    @Test
    void testCanBeDrawn_alwaysTrue() {
        Inventor inv = new Inventor("i", Icon.BOAT, 1, null);
        Player p = new Player("Test");
        assertTrue(inv.canBeDrawn(p));
    }

    @Test
    void testInsert_addsToInventorSet() {
        Inventor inv = new Inventor("i", Icon.BOAT, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        inv.insert(cards);
        assertTrue(cards.get(CardType.INVENTOR).contains(inv));
    }

    @Test
    void testInsert_doesNotAddToOtherSets() {
        Inventor inv = new Inventor("i", Icon.BOAT, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        inv.insert(cards);
        for (CardType type : CardType.values()) {
            if (type != CardType.INVENTOR) {
                assertFalse(cards.get(type).contains(inv));
            }
        }
    }

    private Map<CardType, Set<Card>> initCardsMap() {
        Map<CardType, Set<Card>> cards = new HashMap<>();
        for (CardType t : CardType.values()) cards.put(t, new HashSet<>());
        return cards;
    }
}
