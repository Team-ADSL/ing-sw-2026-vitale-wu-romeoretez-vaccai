package org.example.model.game;
import org.example.model.card.Card;
import org.example.model.game.boardComponent.Deck;
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
        // Arrange - fresh deck and cards before each test
        deck = new Deck();
        card1 = new FakeCard();
        card2 = new FakeCard();
        card3 = new FakeCard();
    }

    // --- isEmpty() tests ---

    @Test
    void testIsEmptyOnNewDeck() {
        // new deck should be empty
        assertTrue(deck.isEmpty());
    }

    @Test
    void testIsEmptyAfterAdd() {
        // deck should not be empty after adding a card
        deck.addTailCard(card1);
        assertFalse(deck.isEmpty());
    }

    // --- addTailCard() tests ---

    @Test
    void testAddTailCardIncreasesSize() {
        // size should grow after each add
        deck.addTailCard(card1);
        assertEquals(1, deck.getCards().size());

        deck.addTailCard(card2);
        assertEquals(2, deck.getCards().size());
    }

    @Test
    void testAddTailCardAddsAtEnd() {
        // card should be added at the end of the deck
        deck.addTailCard(card1);
        deck.addTailCard(card2);
        assertEquals(card2, deck.getCards().getLast());
    }

    // --- addTailCards() tests ---

    @Test
    void testAddTailCardsAddsAll() {
        // all cards in the list should be added
        ArrayList<Card> list = new ArrayList<>(List.of(card1, card2, card3));
        deck.addTailCards(list);
        assertEquals(3, deck.getCards().size());
    }

    @Test
    void testAddTailCardsPreservesOrder() {
        // cards should be added in the same order as the input list
        ArrayList<Card> list = new ArrayList<>(List.of(card1, card2, card3));
        deck.addTailCards(list);
        assertEquals(card1, deck.getCards().get(0));
        assertEquals(card2, deck.getCards().get(1));
        assertEquals(card3, deck.getCards().get(2));
    }

    // --- drawCard() tests ---

    @Test
    void testDrawCardReturnsFirstCard() {
        // should return the first card added
        deck.addTailCard(card1);
        deck.addTailCard(card2);
        assertEquals(card1, deck.drawCard());
    }

    @Test
    void testDrawCardRemovesCard() {
        // deck size should decrease after drawing
        deck.addTailCard(card1);
        deck.drawCard();
        assertTrue(deck.isEmpty());
    }

    @Test
    void testDrawCardPreservesOrder() {
        // cards should be drawn in FIFO order
        deck.addTailCard(card1);
        deck.addTailCard(card2);
        deck.drawCard(); // removes card1
        assertEquals(card2, deck.drawCard()); // card2 is now first
    }

    // --- createDeck() tests ---

    @Test
    void testCreateDeckContainsAllCards() {
        // deck should contain all cards from all eras
        Set<Card> era1 = new HashSet<>(Set.of(card1));
        Set<Card> era2 = new HashSet<>(Set.of(card2));
        Set<Card> era3 = new HashSet<>(Set.of(card3));

        ArrayList<Set<Card>> eras = new ArrayList<>(List.of(era1, era2, era3));
        Deck created = Deck.createDeck(eras);

        assertEquals(3, created.getCards().size());
    }

    @Test
    void testCreateDeckIsNotEmpty() {
        // created deck should not be empty
        Set<Card> era1 = new HashSet<>(Set.of(card1, card2));
        ArrayList<Set<Card>> eras = new ArrayList<>(List.of(era1));
        Deck created = Deck.createDeck(eras);

        assertFalse(created.isEmpty());
    }
}
