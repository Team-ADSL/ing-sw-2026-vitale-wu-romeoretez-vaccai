package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.*;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExtraMoveStateTest {

    private static final GamePersistenceManager NO_OP_PERSISTENCE = new GamePersistenceManager() {
        @Override public List<Game> recoverGames() { return List.of(); }
        @Override public void removeGame(int id) {}
        @Override public void updateLobby(List<String> p) {}
        @Override public void updateGame(Game g) {}
    };
    private static final GameDAO NO_OP_DAO = new GameDAO() {
        @Override public int createMatch() { return 0; }
        @Override public void deleteMatch(int id) {}
        @Override public void saveMatch(int id, int c, List<String> n, List<Integer> s) {}
        @Override public List<MatchResult> getLeaderboard(int c) { return List.of(); }
    };

    private Game game;
    private Player p1;
    private Player p2;
    private ExtraMoveState state;

    @BeforeEach
    void setUp() {
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        game = new Game(1, 5);

        p1 = new Player("p1");
        p2 = new Player("p2");
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        // Minimal board with empty offer/order tracks
        Board board = new Board(
                new CardRow(3, 3),
                new CardRow(6, 6),
                new OfferTrack(new ArrayList<>()),
                new OrderTile("order_tile_5p", new ArrayList<>()),
                new ArrayList<>(),
                new Deck(new ArrayList<>())
        );
        game.setBoard(board);

        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        state = new ExtraMoveState(game, controller);
    }

    // ──────────────────────────────────────────────
    // nextState — no player has extraMove → EventsState
    // ──────────────────────────────────────────────

    @Test
    void nextState_returnsEventsStateWhenNoPlayerHasExtraMove() {
        // By default BuildingBonus.isExtraMove() == false for both players
        ControllerState next = state.calcNextState();
        assertInstanceOf(EventsState.class, next);
    }

    // ──────────────────────────────────────────────
    // nextState — a player has extraMove → self
    // ──────────────────────────────────────────────

    @Test
    void nextState_returnsSelfWhenPlayerHasExtraMove() {
        p1.getBuildingBonus().setExtraMove(true);

        ControllerState next = state.calcNextState();
        assertInstanceOf(ExtraMoveState.class, next);
        assertSame(state, next);
    }

    @Test
    void nextState_setsCurrentPlayerWhenExtraMove() {
        p2.getBuildingBonus().setExtraMove(true);
        state.calcNextState();
        // current player must be set to the one with extra move
        assertTrue(game.getCurrentPlayer().isPresent());
        assertEquals("p2", game.getCurrentPlayer().get().getName());
    }

    @Test
    void nextState_clearsCurrentPlayerWhenNoExtraMove() {
        game.setCurrentPlayer(p1);
        state.calcNextState();
        assertFalse(game.getCurrentPlayer().isPresent());
    }
}
