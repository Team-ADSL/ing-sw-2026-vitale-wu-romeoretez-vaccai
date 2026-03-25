//package org.example.model.game;
//import org.example.model.card.Card;
//import org.example.model.card.util.Trigger;
//import org.example.model.card.building.Building;
//import org.example.model.game.boardComponent.*;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import java.util.*;
//import static org.junit.jupiter.api.Assertions.*;
//
//public class GameTest {
//
//    // ──────────────────────────────────────────────
//    // Stub di Building concreto
//    // ──────────────────────────────────────────────
//
//    static class FakeBuilding extends Building {
//        public FakeBuilding(int era) {
//            super(0, 0, era, Optional.empty());
//        }
//        @Override
//        public void activeEffect(Set<Player> players, Trigger t) {}
//    }
//
//    // ──────────────────────────────────────────────
//    // Helpers
//    // ──────────────────────────────────────────────
//
//    private ArrayList<Set<Card>> deckWithFakeCards(int n) {
//        Set<Card> era1 = new HashSet<>();
//        for (int i = 0; i < n; i++) era1.add(new FakeCard());
//        return new ArrayList<>(List.of(era1));
//    }
//
//    private Board buildBoard() {
//        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());
//        OrderTile orderTile   = new OrderTile(new ArrayList<>());
//
//        // AGGIORNAMENTO: Aggiunto lo '0' finale per il parametro nonBuildingCards
//        // richiesto dal nuovo costruttore Initialiser di Board.
//        return new Board(2, offerTrack, orderTile,
//                new ArrayList<>(), deckWithFakeCards(10), 0);
//    }
//
//    private Set<Player> buildPlayers() {
//        Set<Player> players = new HashSet<>();
//        players.add(new Player("Gianpaolo", 5, 0, Color.RED));
//        players.add(new Player("Gianpiero",   5, 0, Color.BLUE));
//        return players;
//    }
//
//    // ──────────────────────────────────────────────
//    // Fixtures
//    // ──────────────────────────────────────────────
//
//    private Game game;
//    private Board board;
//    private Set<Player> players;
//
//    @BeforeEach
//    void setUp() {
//        board   = buildBoard();
//        players = buildPlayers();
//        game    = new Game(1, 1, players, board);
//    }
//
//    // ──────────────────────────────────────────────
//    // Test costruttore
//    // ──────────────────────────────────────────────
//
//    @Test
//    void constructor_storesAllFieldsCorrectly() {
//        assertAll(
//                () -> assertEquals(1,       game.getRound()),
//                () -> assertEquals(1,       game.getEra()),
//                () -> assertSame(players,   game.getPlayers()),
//                () -> assertSame(board,     game.getBoard())
//        );
//    }
//
//    // ──────────────────────────────────────────────
//    // Test getRound
//    // ──────────────────────────────────────────────
//
//    @Test
//    void getRound_returnsCorrectRound() {
//        Game g = new Game(3, 1, players, board);
//        assertEquals(3, g.getRound());
//    }
//
//    @Test
//    void getRound_roundOne_returnsOne() {
//        assertEquals(1, game.getRound());
//    }
//
//    // ──────────────────────────────────────────────
//    // Test getEra
//    // ──────────────────────────────────────────────
//
//    @Test
//    void getEra_returnsCorrectEra() {
//        Game g = new Game(1, 2, players, board);
//        assertEquals(2, g.getEra());
//    }
//
//    @Test
//    void getEra_eraOne_returnsOne() {
//        assertEquals(1, game.getEra());
//    }
//
//    // ──────────────────────────────────────────────
//    // Test getPlayers
//    // ──────────────────────────────────────────────
//
//    @Test
//    void getPlayers_returnsSameSetReference() {
//        assertSame(players, game.getPlayers());
//    }
//
//    @Test
//    void getPlayers_containsExpectedPlayers() {
//        Set<String> names = new HashSet<>();
//        for (Player p : game.getPlayers()) names.add(p.getName());
//        assertTrue(names.contains("Gianpaolo"));
//        assertTrue(names.contains("Gianpiero"));
//    }
//
//    @Test
//    void getPlayers_returnsCorrectSize() {
//        assertEquals(2, game.getPlayers().size());
//    }
//
//    // ──────────────────────────────────────────────
//    // Test getBoard
//    // ──────────────────────────────────────────────
//
//    @Test
//    void getBoard_returnsSameBoardReference() {
//        assertSame(board, game.getBoard());
//    }
//
//    @Test
//    void getBoard_boardIsNotNull() {
//        assertNotNull(game.getBoard());
//    }
//
//    // ──────────────────────────────────────────────
//    // Test con valori limite
//    // ──────────────────────────────────────────────
//
//    @Test
//    void constructor_withRoundZero_storesZero() {
//        Game g = new Game(0, 1, players, board);
//        assertEquals(0, g.getRound());
//    }
//
//    @Test
//    void constructor_withEraZero_storesZero() {
//        Game g = new Game(1, 0, players, board);
//        assertEquals(0, g.getEra());
//    }
//
//    @Test
//    void constructor_withEmptyPlayerSet_storesEmptySet() {
//        Game g = new Game(1, 1, new HashSet<>(), board);
//        assertTrue(g.getPlayers().isEmpty());
//    }
//}