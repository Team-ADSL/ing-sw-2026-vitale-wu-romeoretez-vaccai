package org.example.server.model.cards.characters;

import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.example.shared.enums.Icon;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class InventorTest {

    @Test
    void getIcon_returnsCorrectValue() {
        Inventor inv = new Inventor("i", Icon.BOAT, 1, null);
        assertEquals(Icon.BOAT, inv.getIcon());
    }

    @Test
    void getIcon_differentIcon_returnsCorrectValue() {
        Inventor inv = new Inventor("i", Icon.TOTEM, 1, null);
        assertEquals(Icon.TOTEM, inv.getIcon());
    }

    @Test
    void getEra_returnsCorrectValue() {
        Inventor inv = new Inventor("i", Icon.ROPE, 2, null);
        assertEquals(2, inv.getEra());
    }

    @Test
    void getId_returnsCorrectId() {
        Inventor inv = new Inventor("inventor_01", Icon.BOAT, 1, null);
        assertEquals("inventor_01", inv.getId());
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Inventor inv = new Inventor("i", Icon.BOAT, 1, null);
        Player p = new Player("Test");
        assertTrue(inv.canBeDrawn(p));
    }

    @Test
    void insert_addsToInventorSet() {
        Inventor inv = new Inventor("i", Icon.BOAT, 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        inv.insert(cards);
        assertTrue(cards.get(CardType.INVENTOR).contains(inv));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
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
