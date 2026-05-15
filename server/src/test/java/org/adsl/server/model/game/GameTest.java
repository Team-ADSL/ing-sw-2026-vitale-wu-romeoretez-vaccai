package org.adsl.server.model.game;

import org.adsl.server.model.*;
import org.adsl.server.model.board.*;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.Phase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

public class GameTest {

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private Board buildBoard() {
        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());
        OrderTile orderTile = new OrderTile("order_tile_5p", new ArrayList<>());
        Set<Card> era1 = new HashSet<>();
        for (int i = 0; i < 5; i++) era1.add(new FakeCard());
        ArrayList<Set<Card>> cards = new ArrayList<>(List.of(era1));
        Deck deck = Deck.createDeck(cards);
        return new Board(3, 3, 6, 6, offerTrack, orderTile, deck);
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
        game = new Game(42, 5, 2, 1, players, null, board, null);
    }

    // ──────────────────────────────────────────────
    // TEST INITIAL CONSTRUCTOR DEFAULTS
    // ──────────────────────────────────────────────

    @Test
    void testInitialConstructor_setsDefaultValues() {
        Game g = new Game(1, 5);
        assertAll(
                () -> assertEquals(1,         g.getGameId()),
                () -> assertEquals(1,         g.getRound()),
                () -> assertEquals(1,         g.getEra()),
                () -> assertTrue(g.getPlayers().isEmpty()),
                () -> assertNull(g.getBoard()),
                () -> assertFalse(g.isInitialized()),
                () -> assertTrue(g.getCurrentPlayer().isEmpty())
        );
    }

    // ──────────────────────────────────────────────
    // TEST RECOVERY CONSTRUCTOR
    // ──────────────────────────────────────────────

    @Test
    void testRecoveryConstructor_storesAllFieldsCorrectly() {
        assertAll(
                () -> assertEquals(42,          game.getGameId()),
                () -> assertEquals(2,           game.getRound()),
                () -> assertEquals(1,           game.getEra()),
                () -> assertSame(players,       game.getPlayers()),
                () -> assertSame(board,         game.getBoard()),
                () -> assertTrue(game.isInitialized()),
                () -> assertTrue(game.getCurrentPlayer().isEmpty())
        );
    }

    // ──────────────────────────────────────────────
    // TEST GET PLAYERS
    // ──────────────────────────────────────────────

    @Test
    void testGetPlayers_containsExpectedPlayers() {
        Set<String> names = new HashSet<>();
        for (Player p : game.getPlayers()) names.add(p.getName());
        assertTrue(names.contains("Gianpaolo"));
        assertTrue(names.contains("Gianpiero"));
    }

    @Test
    void testGetPlayers_returnsCorrectSize() {
        assertEquals(2, game.getPlayers().size());
    }

    // ──────────────────────────────────────────────
    // TEST CHANGE ERA / CHANGE ROUND
    // ──────────────────────────────────────────────

    @Test
    void testChangeEra_incrementsEraByOne() {
        int before = game.getEra();
        game.changeEra();
        assertEquals(before + 1, game.getEra());
    }

    @Test
    void testChangeEra_multipleTimesAccumulates() {
        game.changeEra();
        game.changeEra();
        assertEquals(3, game.getEra());
    }

    @Test
    void testChangeRound_incrementsRoundByOne() {
        int before = game.getRound();
        game.changeRound();
        assertEquals(before + 1, game.getRound());
    }

    @Test
    void testChangeRound_multipleTimesAccumulates() {
        game.changeRound();
        game.changeRound();
        game.changeRound();
        assertEquals(5, game.getRound());
    }

    // ──────────────────────────────────────────────
    // TEST SET BOARD / SET PHASE
    // ──────────────────────────────────────────────

    @Test
    void testSetBoard_updatesBoard() {
        Board newBoard = buildBoard();
        Game g = new Game(1, 2);
        g.setBoard(newBoard);
        assertSame(newBoard, g.getBoard());
    }

    @Test
    void testSetPhase_updatesPhase() {
        game.setPhase(Phase.ACTION_EXECUTION);
        assertEquals(Phase.ACTION_EXECUTION, game.getPhase());
    }

    @Test
    void testSetPhase_null_setsNull() {
        game.setPhase(null);
        assertNull(game.getPhase());
    }

    // ──────────────────────────────────────────────
    // TEST CREATE DTO
    // ──────────────────────────────────────────────

    @Test
    void testCreateDTO_returnsNonNull() {
        assertNotNull(game.createDTO());
    }

    @Test
    void testCreateDTO_containsCorrectGameId() {
        assertEquals(42, game.createDTO().id());
    }
}
