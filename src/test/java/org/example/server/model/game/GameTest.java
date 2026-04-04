package org.example.server.model.game;

import org.example.server.model.*;
import org.example.server.model.board.*;
import org.example.server.model.cards.Card;
import org.example.shared.enums.Phase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class GameTest {

    // ──────────────────────────────────────────────
    // No-op observer stubs
    // ──────────────────────────────────────────────

    private static final EndGameObserver NO_OP_END_GAME = (gameId, results) -> {};
    private static final ModelObserver NO_OP_MODEL = new ModelObserver() {
        @Override public void updateHome(java.util.List<Integer> g) {}
        @Override public void updateLobby(java.util.List<String> p) {}
        @Override public void updateGame(org.example.shared.model.GameDTO g) {}
    };

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private Board buildBoard() {
        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());
        OrderTile orderTile = new OrderTile(new ArrayList<>());
        Set<Card> era1 = new HashSet<>();
        for (int i = 0; i < 5; i++) era1.add(new FakeCard());
        ArrayList<Set<Card>> cards = new ArrayList<>(List.of(era1));
        return new Board(3, 6, offerTrack, orderTile, cards, 3);
    }

    private Set<Player> buildPlayers() {
        Set<Player> players = new HashSet<>();
        players.add(new Player("Gianpaolo"));
        players.add(new Player("Gianpiero"));
        return players;
    }

    // ──────────────────────────────────────────────
    // Fixtures
    // ──────────────────────────────────────────────

    private Game game;
    private Board board;
    private Set<Player> players;

    @BeforeEach
    void setUp() {
        board = buildBoard();
        players = buildPlayers();
        game = new Game(42, 2, 1, players, null, board,
                Phase.LOBBY, false, NO_OP_END_GAME, NO_OP_MODEL);
    }

    // ──────────────────────────────────────────────
    // Initial constructor defaults
    // ──────────────────────────────────────────────

    @Test
    void initialConstructor_setsDefaultValues() {
        Game g = new Game(1, NO_OP_END_GAME);
        assertAll(
                () -> assertEquals(1,         g.getGameId()),
                () -> assertEquals(0,         g.getRound()),
                () -> assertEquals(1,         g.getEra()),
                () -> assertTrue(g.getPlayers().isEmpty()),
                () -> assertNull(g.getBoard()),
                () -> assertEquals(Phase.LOBBY, g.getPhase()),
                () -> assertFalse(g.isInitialized()),
                () -> assertTrue(g.getCurrentPlayer().isEmpty())
        );
    }

    // ──────────────────────────────────────────────
    // Recovery constructor stores all fields
    // ──────────────────────────────────────────────

    @Test
    void recoveryConstructor_storesAllFieldsCorrectly() {
        assertAll(
                () -> assertEquals(42,          game.getGameId()),
                () -> assertEquals(2,           game.getRound()),
                () -> assertEquals(1,           game.getEra()),
                () -> assertSame(players,       game.getPlayers()),
                () -> assertSame(board,         game.getBoard()),
                () -> assertEquals(Phase.LOBBY, game.getPhase()),
                () -> assertFalse(game.isInitialized()),
                () -> assertTrue(game.getCurrentPlayer().isEmpty())
        );
    }

    // ──────────────────────────────────────────────
    // getPlayers
    // ──────────────────────────────────────────────

    @Test
    void getPlayers_containsExpectedPlayers() {
        Set<String> names = new HashSet<>();
        for (Player p : game.getPlayers()) names.add(p.getName());
        assertTrue(names.contains("Gianpaolo"));
        assertTrue(names.contains("Gianpiero"));
    }

    @Test
    void getPlayers_returnsCorrectSize() {
        assertEquals(2, game.getPlayers().size());
    }

    // ──────────────────────────────────────────────
    // changeEra / changeRound
    // ──────────────────────────────────────────────

    @Test
    void changeEra_incrementsEraByOne() {
        int before = game.getEra();
        game.changeEra();
        assertEquals(before + 1, game.getEra());
    }

    @Test
    void changeEra_multipleTimesAccumulates() {
        game.changeEra();
        game.changeEra();
        assertEquals(3, game.getEra());
    }

    @Test
    void changeRound_incrementsRoundByOne() {
        int before = game.getRound();
        game.changeRound();
        assertEquals(before + 1, game.getRound());
    }

    @Test
    void changeRound_multipleTimesAccumulates() {
        game.changeRound();
        game.changeRound();
        game.changeRound();
        assertEquals(5, game.getRound()); // starts at 2
    }

    // ──────────────────────────────────────────────
    // Setters
    // ──────────────────────────────────────────────

    @Test
    void setBoard_updatesBoard() {
        Board newBoard = buildBoard();
        game.setBoard(newBoard);
        assertSame(newBoard, game.getBoard());
    }

    @Test
    void setPhase_updatesPhase() {
        game.setPhase(Phase.ACTION_EXECUTION);
        assertEquals(Phase.ACTION_EXECUTION, game.getPhase());
    }

    @Test
    void setInitialized_trueUpdatesFlag() {
        game.setInitialized(true);
        assertTrue(game.isInitialized());
    }

    @Test
    void setInitialized_falseUpdatesFlag() {
        game.setInitialized(true);
        game.setInitialized(false);
        assertFalse(game.isInitialized());
    }

    @Test
    void setCurrentPlayer_updatesCurrentPlayer() {
        Player p = new Player("TestPlayer");
        game.setCurrentPlayer(p);
        assertTrue(game.getCurrentPlayer().isPresent());
        assertSame(p, game.getCurrentPlayer().get());
    }

    @Test
    void setCurrentPlayer_toEmpty_clearsCurrentPlayer() {
        Player p = new Player("TestPlayer");
        game.setCurrentPlayer(p);
        game.setCurrentPlayer(null);
        assertTrue(game.getCurrentPlayer().isEmpty());
    }
}