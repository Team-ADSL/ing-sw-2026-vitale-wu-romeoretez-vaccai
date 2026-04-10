package org.example.server.model.game;

import org.example.server.model.board.*;
import org.example.server.model.cards.Card;
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
        return new OrderTile(new ArrayList<>());
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
    void constructorWithComponents_storesDependenciesCorrectly() {
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
    void constructorWithComponents_deckIsNotEmptyWhenCardsProvided() {
        Board board = new Board(
                new CardRow(6, 3), new CardRow(6, 3),
                emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), deckWithFakeCards(5));

        assertFalse(board.deck().isEmpty());
    }

    @Test
    void constructorWithComponents_deckIsEmptyWhenNoCardsProvided() {
        Board board = new Board(
                new CardRow(6, 3), new CardRow(6, 3),
                emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), new Deck(new ArrayList<>()));

        assertTrue(board.deck().isEmpty());
    }

    @Test
    void constructorWithComponents_deckContainsAllProvidedCards() {
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
    void initConstructor_rowSizesMatchParameters() {
        Board board = new Board(6, 3, 6, 3, emptyOfferTrack(), emptyOrderTile(),
                fakeCards(10));

        assertAll(
                () -> assertEquals(6, board.lowRow().size()),
                () -> assertEquals(6, board.topRow().size())
        );
    }

    @Test
    void initConstructor_remainingBuildingsStartsEmpty() {
        Board board = new Board(3, 3,6, 6, emptyOfferTrack(), emptyOrderTile(),
                fakeCards(10));

        assertTrue(board.remainingBuildings().isEmpty());
    }

    @Test
    void initConstructor_deckIsNotEmptyWhenCardsProvided() {
        Board board = new Board(3, 3,6, 6, emptyOfferTrack(), emptyOrderTile(),
                fakeCards(10));

        assertFalse(board.deck().isEmpty());
    }

    // ──────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────

    @Test
    void getters_returnCorrectReferences() {
        CardRow low = new CardRow(6, 3);
        CardRow top = new CardRow(6, 3);
        OfferTrack ot = emptyOfferTrack();
        OrderTile oq = emptyOrderTile();
        ArrayList<Set<Card>> remaining = new ArrayList<>();

        Board board = new Board(low, top, ot, oq, remaining, deckWithFakeCards(5));

        assertAll(
                () -> assertSame(ot,        board.offerTrack()),
                () -> assertSame(oq,        board.orderTile()),
                () -> assertSame(low,       board.lowRow()),
                () -> assertSame(top,       board.topRow()),
                () -> assertSame(remaining, board.remainingBuildings()),
                () -> assertNotNull(board.deck())
        );
    }
}