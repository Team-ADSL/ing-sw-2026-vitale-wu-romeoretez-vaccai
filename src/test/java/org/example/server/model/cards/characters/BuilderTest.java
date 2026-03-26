package org.example.server.model.cards.characters;

import org.example.server.model.cards.Card;
import org.example.server.model.cards.characters.Builder;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Color;
import org.example.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BuilderTest {

    @Test
    void getDiscount_returnsCorrectValue() {
        Builder b = new Builder(2, 5, 1, Optional.empty());
        assertEquals(2, b.getDiscount());
    }

    @Test
    void getPP_returnsCorrectValue() {
        Builder b = new Builder(2, 5, 1, Optional.empty());
        assertEquals(5, b.getPP());
    }

    @Test
    void getEra_returnsCorrectValue() {
        Builder b = new Builder(2, 5, 3, Optional.empty());
        assertEquals(3, b.getEra());
    }

    @Test
    void getNumPlayers_returnsCorrectValue() {
        Builder b = new Builder(2, 5, 1, Optional.of(4));
        assertEquals(Optional.of(4), b.getNumPlayers());
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Builder b = new Builder(2, 5, 1, Optional.empty());
        Player p = new Player("Test", 0, 0, Color.RED);
        assertTrue(b.canBeDrawn(p));
    }

    @Test
    void insert_addsToBuilderSet() {
        Builder b = new Builder(2, 5, 1, Optional.empty());
        Map<CardType, Set<Card>> cards = initCardsMap();
        b.insert(cards);
        assertTrue(cards.get(CardType.BUILDER).contains(b));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
        Builder b = new Builder(2, 5, 1, Optional.empty());
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
