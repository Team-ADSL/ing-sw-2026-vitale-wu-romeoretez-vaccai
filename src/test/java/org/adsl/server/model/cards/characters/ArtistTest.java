package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.Player;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ArtistTest {

    @Test
    void getEra_returnsCorrectValue() {
        Artist a = new Artist("a", 2, null);
        assertEquals(2, a.getEra());
    }

    @Test
    void getId_returnsCorrectId() {
        Artist a = new Artist("artist_01", 1, null);
        assertEquals("artist_01", a.getId());
    }

    @Test
    void canBeDrawn_alwaysTrue() {
        Artist a = new Artist("a", 1, null);
        Player p = new Player("Test");
        assertTrue(a.canBeDrawn(p));
    }

    @Test
    void insert_addsToArtistSet() {
        Artist a = new Artist("a", 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        a.insert(cards);
        assertTrue(cards.get(CardType.ARTIST).contains(a));
    }

    @Test
    void insert_doesNotAddToOtherSets() {
        Artist a = new Artist("a", 1, null);
        Map<CardType, Set<Card>> cards = initCardsMap();
        a.insert(cards);
        for (CardType type : CardType.values()) {
            if (type != CardType.ARTIST) {
                assertFalse(cards.get(type).contains(a));
            }
        }
    }

    private Map<CardType, Set<Card>> initCardsMap() {
        Map<CardType, Set<Card>> cards = new HashMap<>();
        for (CardType t : CardType.values()) cards.put(t, new HashSet<>());
        return cards;
    }
}
