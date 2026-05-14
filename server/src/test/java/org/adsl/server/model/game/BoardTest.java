package org.adsl.server.model.game;

import org.adsl.server.model.board.*;
import org.adsl.server.model.cards.Card;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private OfferTrack emptyOfferTrack() {
        return new OfferTrack(new ArrayList<>());
    }

    private OrderTile emptyOrderTile() {
        return new OrderTile("order_tile_5p", new ArrayList<>());
    }

    private ArrayList<Set<Card>> fakeCards(int n) {
        Set<Card> era1 = new HashSet<>();
        for (int i = 0; i < n; i++) era1.add(new FakeCard());
        return new ArrayList<>(List.of(era1));
    }

    private Deck deckWithFakeCards(int n) {
        ArrayList<Card> era1 = new ArrayList<>();
        for (int i = 0; i < n; i++) era1.add(new FakeCard());
        return new Deck(era1);
    }

    // ──────────────────────────────────────────────
    // Constructor 1: Board(CardRow, CardRow, OfferTrack, OrderTile, remainingBuildings, cards)
    // ──────────────────────────────────────────────

    @Test
    void testConstructorWithComponents_storesDependenciesCorrectly() {
        CardRow low = new CardRow(6, 3);
        CardRow top = new CardRow(6, 3);
        OfferTrack ot = emptyOfferTrack();
        OrderTile oq = emptyOrderTile();
        ArrayList<Set<Card>> remaining = new ArrayList<>();

        Board board = new Board(low, top, ot, oq, remaining, deckWithFakeCards(5));

        assertAll(
                () -> assertSame(low,       board.lowRow()),
                () -> assertSame(top,       board.topRow()),
                () -> assertSame(ot,        board.offerTrack()),
                () -> assertSame(oq,        board.orderTile()),
                () -> assertSame(remaining, board.remainingBuildings()),
                () -> assertNotNull(board.deck())
        );
    }

    @Test
    void testConstructorWithComponents_deckIsNotEmptyWhenCardsProvided() {
        Board board = new Board(
                new CardRow(6, 3), new CardRow(6, 3),
                emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), deckWithFakeCards(5));

        assertFalse(board.deck().isEmpty());
    }

    @Test
    void testConstructorWithComponents_deckIsEmptyWhenNoCardsProvided() {
        Board board = new Board(
                new CardRow(6, 3), new CardRow(6, 3),
                emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), new Deck(new ArrayList<>()));

        assertTrue(board.deck().isEmpty());
    }

    @Test
    void testConstructorWithComponents_deckContainsAllProvidedCards() {
        int cardCount = 8;
        Board board = new Board(
                new CardRow(6, 3), new CardRow(6, 3),
                emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), deckWithFakeCards(cardCount));

        assertEquals(cardCount, board.deck().cards().size());
    }

    // ──────────────────────────────────────────────
    // Constructor 2: Board(numLowCard, numTopCard, OfferTrack, OrderTile, cards, nonBuildingCards)
    // ──────────────────────────────────────────────

    @Test
    void testInitConstructor_rowSizesMatchParameters() {
        Deck fakeDeck = Deck.createDeck(fakeCards(10));
        Board board = new Board(6, 3, 6, 3,
                emptyOfferTrack(), emptyOrderTile(), fakeDeck);

        assertAll(
                () -> assertEquals(6, board.lowRow().size()),
                () -> assertEquals(6, board.topRow().size())
        );
    }

    @Test
    void testInitConstructor_remainingBuildingsStartsEmpty() {
        Deck fakeDeck = Deck.createDeck(fakeCards(10));
        Board board = new Board(3, 3,6, 6,
                emptyOfferTrack(), emptyOrderTile(), fakeDeck);

        assertTrue(board.remainingBuildings().isEmpty());
    }

    @Test
    void testInitConstructor_deckIsNotEmptyWhenCardsProvided() {
        Deck fakeDeck = Deck.createDeck(fakeCards(10));

        Board board = new Board(3, 3,6, 6,
                emptyOfferTrack(), emptyOrderTile(), fakeDeck);

        assertFalse(board.deck().isEmpty());
    }
}
