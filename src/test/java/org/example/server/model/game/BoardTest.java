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

    private ArrayList<Set<Card>> deckWithFakeCards(int n) {
        Set<Card> era1 = new HashSet<>();
        for (int i = 0; i < n; i++) era1.add(new FakeCard());
        return new ArrayList<>(List.of(era1));
    }

    // ──────────────────────────────────────────────
    // Constructor 1: Board(CardRow, CardRow, OfferTrack, OrderTile, remainingBuildings, cards)
    // ──────────────────────────────────────────────

    @Test
    void constructorWithComponents_storesDependenciesCorrectly() {
        CardRow low = new CardRow(3, 3);
        CardRow top = new CardRow(6, 6);
        OfferTrack ot = emptyOfferTrack();
        OrderTile oq = emptyOrderTile();
        ArrayList<Set<Card>> remaining = new ArrayList<>();

        Board board = new Board(low, top, ot, oq, remaining, deckWithFakeCards(5));

        assertAll(
                () -> assertSame(low,       board.getLowRow()),
                () -> assertSame(top,       board.getTopRow()),
                () -> assertSame(ot,        board.getOfferTrack()),
                () -> assertSame(oq,        board.getOrderTile()),
                () -> assertSame(remaining, board.getRemainingBuildings()),
                () -> assertNotNull(board.getDeck())
        );
    }

    @Test
    void constructorWithComponents_deckIsNotEmptyWhenCardsProvided() {
        Board board = new Board(
                new CardRow(3, 3), new CardRow(6, 6),
                emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), deckWithFakeCards(5));

        assertFalse(board.getDeck().isEmpty());
    }

    @Test
    void constructorWithComponents_deckIsEmptyWhenNoCardsProvided() {
        Board board = new Board(
                new CardRow(3, 3), new CardRow(6, 6),
                emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), new ArrayList<>());

        assertTrue(board.getDeck().isEmpty());
    }

    @Test
    void constructorWithComponents_deckContainsAllProvidedCards() {
        int cardCount = 8;
        Board board = new Board(
                new CardRow(3, 3), new CardRow(6, 6),
                emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), deckWithFakeCards(cardCount));

        assertEquals(cardCount, board.getDeck().getCards().size());
    }

    // ──────────────────────────────────────────────
    // Constructor 2: Board(numLowCard, numTopCard, OfferTrack, OrderTile, cards, nonBuildingCards)
    // ──────────────────────────────────────────────

    @Test
    void initConstructor_rowSizesMatchParameters() {
        Board board = new Board(3, 6, emptyOfferTrack(), emptyOrderTile(),
                deckWithFakeCards(10), 3);

        assertAll(
                () -> assertEquals(3, board.getLowRow().size()),
                () -> assertEquals(6, board.getTopRow().size())
        );
    }

    @Test
    void initConstructor_remainingBuildingsStartsEmpty() {
        Board board = new Board(3, 6, emptyOfferTrack(), emptyOrderTile(),
                deckWithFakeCards(10), 3);

        assertTrue(board.getRemainingBuildings().isEmpty());
    }

    @Test
    void initConstructor_deckIsNotEmptyWhenCardsProvided() {
        Board board = new Board(3, 6, emptyOfferTrack(), emptyOrderTile(),
                deckWithFakeCards(10), 3);

        assertFalse(board.getDeck().isEmpty());
    }

    // ──────────────────────────────────────────────
    // Getters
    // ──────────────────────────────────────────────

    @Test
    void getters_returnCorrectReferences() {
        CardRow low = new CardRow(3, 3);
        CardRow top = new CardRow(6, 6);
        OfferTrack ot = emptyOfferTrack();
        OrderTile oq = emptyOrderTile();
        ArrayList<Set<Card>> remaining = new ArrayList<>();

        Board board = new Board(low, top, ot, oq, remaining, deckWithFakeCards(5));

        assertAll(
                () -> assertSame(ot,        board.getOfferTrack()),
                () -> assertSame(oq,        board.getOrderTile()),
                () -> assertSame(low,       board.getLowRow()),
                () -> assertSame(top,       board.getTopRow()),
                () -> assertSame(remaining, board.getRemainingBuildings()),
                () -> assertNotNull(board.getDeck())
        );
    }
}