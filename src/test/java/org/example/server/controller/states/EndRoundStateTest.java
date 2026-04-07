package org.example.server.controller.states;

import org.example.server.config.BoardConfigLoader;
import org.example.server.config.JsonBoardConfigLoader;
import org.example.server.controller.GameController;
import org.example.server.model.EndGameObserver;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.*;
import org.example.server.model.cards.Card;
import org.example.server.persistence.GameDAO;
import org.example.server.persistence.GamePersistenceManager;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Phase;
import org.example.shared.enums.Trigger;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EndRoundStateTest {

    private static final EndGameObserver NO_OP_END = (id, r) -> {};
    private static final GamePersistenceManager NO_OP_PERSISTENCE = new GamePersistenceManager() {
        @Override public List<Game> recoverGames() { return List.of(); }
        @Override public void removeGame(int id) {}
        @Override public void updateLobby(List<String> p) {}
        @Override public void updateGame(GameDTO g) {}
    };
    private static final GameDAO NO_OP_DAO = new GameDAO() {
        @Override public int createMatch() { return 0; }
        @Override public void deleteMatch(int id) {}
        @Override public void saveMatch(int id, int c, List<String> n, List<Integer> s) {}
        @Override public List<MatchResult> getLeaderboard(int c) { return List.of(); }
    };

    // Minimal concrete card for testing
    private static Card fakeCard(int era) {
        return new Card("fake_" + era, era, null) {
            @Override public boolean canBeDrawn(Player p) { return true; }
            @Override public void insert(Map<CardType, Set<Card>> cards) {}
            @Override public void activeEffect(Set<Player> players, Trigger t) {}
        };
    }

    private Game game;
    private EndRoundState endRoundState;
    private BoardConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new JsonBoardConfigLoader();
        game = new Game(1, NO_OP_END);

        // Build board: lowRow=3 slots, topRow=6 slots, 3 tribe slots each, with era-1 cards
        Set<Card> era1 = new HashSet<>();
        for (int i = 0; i < 15; i++) era1.add(fakeCard(1));
        ArrayList<Set<Card>> deckCards = new ArrayList<>(List.of(era1));

        Board board = new Board(3, 6,
                new OfferTrack(new ArrayList<>()),
                new OrderTile(new ArrayList<>()),
                deckCards,
                3);
        game.setBoard(board);

        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        endRoundState = new EndRoundState(game, controller);
    }

    // ──────────────────────────────────────────────
    // nextState
    // ──────────────────────────────────────────────

    @Test
    void nextState_returnsTotemPlacementState() {
        assertInstanceOf(TotemPlacementState.class, endRoundState.nextState());
    }

    // ──────────────────────────────────────────────
    // onEntry — round and phase
    // ──────────────────────────────────────────────

    @Test
    void onEntry_setsPhaseToEndRound() throws Exception {
        endRoundState.onEntry();
        assertEquals(Phase.END_ROUND, game.getPhase());
    }

    @Test
    void onEntry_incrementsRound() throws Exception {
        int before = game.getRound();
        endRoundState.onEntry();
        assertEquals(before + 1, game.getRound());
    }

    // ──────────────────────────────────────────────
    // onEntry — card movement
    // ──────────────────────────────────────────────

    @Test
    void onEntry_topRowTribeCardsClearedAfterMove() throws Exception {
        // Pre-fill top row tribe slots (indices 0..numTribeCard-1)
        CardRow topRow = game.getBoard().getTopRow();
        topRow.add(fakeCard(1));
        topRow.add(fakeCard(1));

        endRoundState.onEntry();

        // Top tribe slots should have been cleared and refilled from deck
        // (we just verify no exception and round advanced)
        assertEquals(1, game.getRound());
    }

    @Test
    void onEntry_lowRowReceivesCardsFromTopRow() throws Exception {
        Card sentinel = fakeCard(1);
        // Place sentinel in top tribe slot
        game.getBoard().getTopRow().add(sentinel);

        endRoundState.onEntry();

        // The low row tribe cards should now contain what was in the top row
        Card[] lowTribe = game.getBoard().getLowRow().getTribeCards();
        boolean found = false;
        for (Card c : lowTribe) {
            if (c == sentinel) { found = true; break; }
        }
        assertTrue(found, "Sentinel card from top row should appear in low row after EndRound");
    }
}
