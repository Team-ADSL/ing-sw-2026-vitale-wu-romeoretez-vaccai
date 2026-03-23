package org.example.model.game;
import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.Building;
import org.example.model.game.boardComponent.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class BoardTest {

    // ──────────────────────────────────────────────
    // Stub carte
    // ──────────────────────────────────────────────

    static class NormalCard extends Card {
        public NormalCard(int era) { super(era, Optional.empty()); }
        @Override public boolean canBeDrawn(Player p) { return true; }
        @Override public void insert(Map<CardType, Set<Card>> cards) {}
        @Override public void activeEffect(Set<Player> players, Trigger t) {}
    }

    static class EventCard extends Card {
        public EventCard(int era) { super(era, Optional.empty()); }
        @Override public boolean canBeDrawn(Player p) { return false; }
        @Override public void insert(Map<CardType, Set<Card>> cards) {}
        @Override public void activeEffect(Set<Player> players, Trigger t) {}
    }

    static class TestBuilding extends Building {
        public TestBuilding(int era) { super(0, 0, era, Optional.empty()); }
        @Override public void activeEffect(Set<Player> players, Trigger t) {}
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private OfferTrack emptyOfferTrack() {
        return new OfferTrack(new ArrayList<>());
    }

    private OrderTile emptyOrderTile() {
        return new OrderTile(new ArrayList<>());
    }

    private ArrayList<Set<Card>> deckWithNormalCards(int n) {
        Set<Card> era1 = new HashSet<>();
        for (int i = 0; i < n; i++) era1.add(new NormalCard(1));
        return new ArrayList<>(List.of(era1));
    }

    private ArrayList<Set<Card>> deckWithMixedCards(int normalCount, int eventCount) {
        Set<Card> era1 = new HashSet<>();
        for (int i = 0; i < normalCount; i++) era1.add(new NormalCard(1));
        for (int i = 0; i < eventCount; i++) era1.add(new EventCard(1));
        return new ArrayList<>(List.of(era1));
    }

    private Set<Building> buildingSet(int era1Count, int era2Count, int era3Count) {
        Set<Building> set = new HashSet<>();
        for (int i = 0; i < era1Count; i++) set.add(new TestBuilding(1));
        for (int i = 0; i < era2Count; i++) set.add(new TestBuilding(2));
        for (int i = 0; i < era3Count; i++) set.add(new TestBuilding(3));
        return set;
    }

    private CardRow newCardRow(int capacity) {
        return new CardRow(capacity, capacity);
    }

    // Board inizializzatore con nonBuildingCards = capacity
    private Board initBoard(int numPlayers, ArrayList<Set<Card>> cards) {
        return new Board(numPlayers, emptyOfferTrack(), emptyOrderTile(),
                new ArrayList<>(), cards, numPlayers + 1);
    }

    // ──────────────────────────────────────────────
    // Test costruttori
    // ──────────────────────────────────────────────

    @Test
    void constructorWithComponents_storesDependenciesCorrectly() {
        CardRow low = newCardRow(3);
        CardRow top = newCardRow(6);
        OfferTrack ot = emptyOfferTrack();
        OrderTile oq = emptyOrderTile();
        ArrayList<Set<Building>> rb = new ArrayList<>();

        Board board = new Board(low, top, ot, oq, rb, deckWithNormalCards(5));

        assertAll(
                () -> assertSame(low, board.getLowRow()),
                () -> assertSame(top, board.getTopRow()),
                () -> assertSame(ot,  board.getOfferTrack()),
                () -> assertSame(oq,  board.getOrderTile()),
                () -> assertNotNull(board.getDeck())
        );
    }

    @Test
    void initConstructor_rowsStartEmpty() {
        Board board = initBoard(2, deckWithNormalCards(20));

        assertEquals(0, board.getLowRow().size());
        assertEquals(0, board.getTopRow().size());
    }

    @Test
    void initConstructor_deckIsNotEmpty() {
        Board board = initBoard(2, deckWithNormalCards(10));

        assertFalse(board.getDeck().isEmpty());
    }

    // ──────────────────────────────────────────────
    // Test calcNumTopCard / calcNumLowCard
    // ──────────────────────────────────────────────

    @Test
    void calcNumTopCard_returnsNumPlayersPlus4() {
        Board board = new Board(newCardRow(3), newCardRow(6),
                emptyOfferTrack(), emptyOrderTile(), new ArrayList<>(), deckWithNormalCards(5));
        assertAll(
                () -> assertEquals(6, board.calcNumTopCard(2)),
                () -> assertEquals(7, board.calcNumTopCard(3)),
                () -> assertEquals(8, board.calcNumTopCard(4)),
                () -> assertEquals(9, board.calcNumTopCard(5))
        );
    }

    @Test
    void calcNumLowCard_returnsNumPlayersPlus1() {
        Board board = new Board(newCardRow(3), newCardRow(6),
                emptyOfferTrack(), emptyOrderTile(), new ArrayList<>(), deckWithNormalCards(5));
        assertAll(
                () -> assertEquals(3, board.calcNumLowCard(2)),
                () -> assertEquals(4, board.calcNumLowCard(3)),
                () -> assertEquals(5, board.calcNumLowCard(4)),
                () -> assertEquals(6, board.calcNumLowCard(5))
        );
    }

    // ──────────────────────────────────────────────
    // Test fillLowRow
    // ──────────────────────────────────────────────

    @Test
    void fillLowRow_placesExactlyNumPlayersPlusOneNormalCards() {
        int numPlayers = 3;
        Board board = initBoard(numPlayers, deckWithNormalCards(20));

        board.fillLowRow(numPlayers);

        assertEquals(numPlayers + 1, board.getLowRow().size());
    }

    @Test
    void fillLowRow_redirectsEventCardsToTopRow() {
        int numPlayers = 2;
        // 3 carte normali (riempiono lowRow) + 2 eventi
        Board board = initBoard(numPlayers, deckWithMixedCards(3, 2));

        board.fillLowRow(numPlayers);

        assertEquals(numPlayers + 1, board.getLowRow().size());
        assertEquals(2, board.getTopRow().size());
    }

    @Test
    void fillLowRow_stopsWhenDeckIsEmpty() {
        int numPlayers = 4;
        Board board = initBoard(numPlayers, deckWithNormalCards(2));

        board.fillLowRow(numPlayers);

        assertTrue(board.getLowRow().size() <= numPlayers + 1);
        assertTrue(board.getDeck().isEmpty());
    }

    @Test
    void fillLowRow_withOnlyEventCards_lowRowRemainsEmpty() {
        int numPlayers = 2;
        Board board = initBoard(numPlayers, deckWithMixedCards(0, 5));

        board.fillLowRow(numPlayers);

        assertEquals(0, board.getLowRow().size());
    }

    // ──────────────────────────────────────────────
    // Test fillTopRow
    // ──────────────────────────────────────────────

    @Test
    void fillTopRow_fillsTopRowToNumPlayersPlusFour() {
        int numPlayers = 3;
        Board board = initBoard(numPlayers, deckWithNormalCards(20));

        board.fillTopRow(numPlayers);

        assertEquals(numPlayers + 4, board.getTopRow().size());
    }

    @Test
    void fillTopRow_doesNotExceedTargetWhenTopRowAlreadyPartiallyFull() {
        int numPlayers = 2;
        // 2 eventi finiscono in topRow durante fillLowRow
        Board board = initBoard(numPlayers, deckWithMixedCards(10, 2));

        board.fillLowRow(numPlayers);
        board.fillTopRow(numPlayers);

        assertEquals(numPlayers + 4, board.getTopRow().size());
    }

    @Test
    void fillTopRow_stopsWhenDeckIsEmpty() {
        int numPlayers = 3;
        Board board = initBoard(numPlayers, deckWithNormalCards(2));

        board.fillTopRow(numPlayers);

        assertTrue(board.getTopRow().size() <= numPlayers + 4);
        assertTrue(board.getDeck().isEmpty());
    }

    // ──────────────────────────────────────────────
    // Test makeBuildingDecks
    // ──────────────────────────────────────────────

    @Test
    void makeBuildingDecks_for2Players_era1GoesToTopRow() {
        Board board = new Board(newCardRow(3), newCardRow(10),
                emptyOfferTrack(), emptyOrderTile(), new ArrayList<>(), deckWithNormalCards(5));

        board.makeBuildingDecks(buildingSet(5, 5, 5), 2); // era1=1

        assertEquals(1, board.getTopRow().size());
    }

    @Test
    void makeBuildingDecks_for3Players_era1GoesToTopRow() {
        Board board = new Board(newCardRow(4), newCardRow(11),
                emptyOfferTrack(), emptyOrderTile(), new ArrayList<>(), deckWithNormalCards(5));

        board.makeBuildingDecks(buildingSet(5, 5, 5), 3); // era1=2

        assertEquals(2, board.getTopRow().size());
    }

    @Test
    void makeBuildingDecks_for4Players_era1GoesToTopRow() {
        Board board = new Board(newCardRow(5), newCardRow(12),
                emptyOfferTrack(), emptyOrderTile(), new ArrayList<>(), deckWithNormalCards(5));

        board.makeBuildingDecks(buildingSet(5, 5, 5), 4); // era1=2

        assertEquals(2, board.getTopRow().size());
    }

    @Test
    void makeBuildingDecks_for5Players_era1GoesToTopRow() {
        Board board = new Board(newCardRow(6), newCardRow(13),
                emptyOfferTrack(), emptyOrderTile(), new ArrayList<>(), deckWithNormalCards(5));

        board.makeBuildingDecks(buildingSet(5, 5, 5), 5); // era1=2

        assertEquals(2, board.getTopRow().size());
    }

    @Test
    void makeBuildingDecks_for2Players_storesCorrectEra2AndEra3() {
        ArrayList<Set<Building>> remainingBuildings = new ArrayList<>();
        Board board = new Board(newCardRow(3), newCardRow(10),
                emptyOfferTrack(), emptyOrderTile(), remainingBuildings, deckWithNormalCards(5));

        board.makeBuildingDecks(buildingSet(5, 5, 5), 2); // era2=2, era3=3

        assertAll(
                () -> assertEquals(2, remainingBuildings.size()),
                () -> assertEquals(2, remainingBuildings.get(0).size(), "era2 deve avere 2 edifici"),
                () -> assertEquals(3, remainingBuildings.get(1).size(), "era3 deve avere 3 edifici")
        );
    }

    @Test
    void makeBuildingDecks_for3Players_storesCorrectEra2AndEra3() {
        ArrayList<Set<Building>> remainingBuildings = new ArrayList<>();
        Board board = new Board(newCardRow(4), newCardRow(11),
                emptyOfferTrack(), emptyOrderTile(), remainingBuildings, deckWithNormalCards(5));

        board.makeBuildingDecks(buildingSet(5, 5, 5), 3); // era2=2, era3=4

        assertAll(
                () -> assertEquals(2, remainingBuildings.size()),
                () -> assertEquals(2, remainingBuildings.get(0).size(), "era2 deve avere 2 edifici"),
                () -> assertEquals(4, remainingBuildings.get(1).size(), "era3 deve avere 4 edifici")
        );
    }

    @Test
    void makeBuildingDecks_for4Players_storesCorrectEra2AndEra3() {
        ArrayList<Set<Building>> remainingBuildings = new ArrayList<>();
        Board board = new Board(newCardRow(5), newCardRow(12),
                emptyOfferTrack(), emptyOrderTile(), remainingBuildings, deckWithNormalCards(5));

        board.makeBuildingDecks(buildingSet(5, 5, 5), 4); // era2=3, era3=4

        assertAll(
                () -> assertEquals(2, remainingBuildings.size()),
                () -> assertEquals(3, remainingBuildings.get(0).size(), "era2 deve avere 3 edifici"),
                () -> assertEquals(4, remainingBuildings.get(1).size(), "era3 deve avere 4 edifici")
        );
    }

    @Test
    void makeBuildingDecks_for5Players_storesCorrectEra2AndEra3() {
        ArrayList<Set<Building>> remainingBuildings = new ArrayList<>();
        Board board = new Board(newCardRow(6), newCardRow(13),
                emptyOfferTrack(), emptyOrderTile(), remainingBuildings, deckWithNormalCards(5));

        board.makeBuildingDecks(buildingSet(5, 5, 5), 5); // era2=3, era3=5

        assertAll(
                () -> assertEquals(2, remainingBuildings.size()),
                () -> assertEquals(3, remainingBuildings.get(0).size(), "era2 deve avere 3 edifici"),
                () -> assertEquals(5, remainingBuildings.get(1).size(), "era3 deve avere 5 edifici")
        );
    }

    // ──────────────────────────────────────────────
    // Test getter
    // ──────────────────────────────────────────────

    @Test
    void getters_returnCorrectReferences() {
        CardRow low = newCardRow(3);
        CardRow top = newCardRow(6);
        OfferTrack ot = emptyOfferTrack();
        OrderTile oq = emptyOrderTile();

        Board board = new Board(low, top, ot, oq, new ArrayList<>(), deckWithNormalCards(5));

        assertAll(
                () -> assertSame(ot,  board.getOfferTrack()),
                () -> assertSame(oq,  board.getOrderTile()),
                () -> assertNotNull(board.getDeck()),
                () -> assertSame(low, board.getLowRow()),
                () -> assertSame(top, board.getTopRow())
        );
    }
}