package org.adsl.server.model.game;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.board.Deck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class DeckTest {

    private Deck deck;
    private FakeCard card1;
    private FakeCard card2;
    private FakeCard card3;

    @BeforeEach
    void setUp() {
        deck = new Deck();
        card1 = new FakeCard();
        card2 = new FakeCard();
        card3 = new FakeCard();
    }

    // ──────────────────────────────────────────────
    // TEST IS EMPTY
    // ──────────────────────────────────────────────

    @Test
    void testIsEmpty_newDeck_returnsTrue() {
        assertTrue(deck.isEmpty());
    }

    @Test
    void testIsEmpty_afterAdd_returnsFalse() {
        deck.addTailCard(card1);
        assertFalse(deck.isEmpty());
    }

    // ──────────────────────────────────────────────
    // TEST ADD TAIL CARD
    // ──────────────────────────────────────────────

    @Test
    void testAddTailCard_increasesSize() {
        deck.addTailCard(card1);
        assertEquals(1, deck.cards().size());

        deck.addTailCard(card2);
        assertEquals(2, deck.cards().size());
    }

    @Test
    void testAddTailCard_addsAtEnd() {
        deck.addTailCard(card1);
        deck.addTailCard(card2);
        assertEquals(card2, deck.cards().getLast());
    }

    // ──────────────────────────────────────────────
    // TEST ADD TAIL CARDS
    // ──────────────────────────────────────────────

    @Test
    void testAddTailCards_addsAll() {
        ArrayList<Card> list = new ArrayList<>(List.of(card1, card2, card3));
        deck.addTailCards(list);
        assertEquals(3, deck.cards().size());
    }

    @Test
    void testAddTailCards_preservesOrder() {
        ArrayList<Card> list = new ArrayList<>(List.of(card1, card2, card3));
        deck.addTailCards(list);
        assertEquals(card1, deck.cards().get(0));
        assertEquals(card2, deck.cards().get(1));
        assertEquals(card3, deck.cards().get(2));
    }

    // ──────────────────────────────────────────────
    // TEST DRAW CARD
    // ──────────────────────────────────────────────

    @Test
    void testDrawCard_returnsFirstCard() {
        deck.addTailCard(card1);
        deck.addTailCard(card2);
        assertEquals(card1, deck.drawCard());
    }

    @Test
    void testDrawCard_removesCard() {
        deck.addTailCard(card1);
        deck.drawCard();
        assertTrue(deck.isEmpty());
    }

    @Test
    void testDrawCard_preservesOrder() {
        deck.addTailCard(card1);
        deck.addTailCard(card2);
        deck.drawCard();
        assertEquals(card2, deck.drawCard());
    }

    // ──────────────────────────────────────────────
    // TEST CREATE DECK
    // ──────────────────────────────────────────────

    @Test
    void testCreateDeck_containsAllCards() {
        Set<Card> era1 = new HashSet<>(Set.of(card1));
        Set<Card> era2 = new HashSet<>(Set.of(card2));
        Set<Card> era3 = new HashSet<>(Set.of(card3));

        ArrayList<Set<Card>> eras = new ArrayList<>(List.of(era1, era2, era3));
        Deck created = Deck.createDeck(eras);

        assertEquals(3, created.cards().size());
    }

    @Test
    void testCreateDeck_isNotEmpty() {
        Set<Card> era1 = new HashSet<>(Set.of(card1, card2));
        ArrayList<Set<Card>> eras = new ArrayList<>(List.of(era1));
        Deck created = Deck.createDeck(eras);

        assertFalse(created.isEmpty());
    }
}
