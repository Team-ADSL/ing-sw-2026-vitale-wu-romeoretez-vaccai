package org.adsl.server.controller;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.states.*;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.*;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class StateFactoryTest {

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

    private BoardConfigLoader loader;

    /** Build a minimal Board so states that call getBoard() don't NPE. */
    private Board minimalBoard() {
        return new Board(
                new CardRow(3, 3),
                new CardRow(6, 6),
                new OfferTrack(new ArrayList<>()),
                new OrderTile("order_tile_5p", new ArrayList<>()),
                new ArrayList<>(),
                new Deck(new ArrayList<>())
        );
    }

    /** Create a game in the given phase with a minimal board. */
    private Game gameInPhase(Phase phase) {
        Set<Player> players = new HashSet<>();
        Game g = new Game(1, 5, 0, 1, players, null, minimalBoard(), phase);
        return g;
    }

    @BeforeEach
    void setUp() {
        loader = new JsonBoardConfigLoader();
    }

    private GameController makeController(Game g) {
        return new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
    }

    // ──────────────────────────────────────────────
    // StateFactory.recover — each Phase
    // ──────────────────────────────────────────────

    @Test
    void recover_TOTEM_PLACEMENT_returnsTotemPlacementState() {
        Game g = gameInPhase(Phase.TOTEM_PLACEMENT);
        ControllerState s = StateFactory.recover(g, makeController(g));
        assertInstanceOf(TotemPlacementState.class, s);
    }

    @Test
    void recover_ACTION_EXECUTION_returnsActionExecutionState() {
        Game g = gameInPhase(Phase.ACTION_EXECUTION);
        ControllerState s = StateFactory.recover(g, makeController(g));
        assertInstanceOf(ActionExecutionState.class, s);
    }

    @Test
    void recover_EXTRA_MOVE_returnsExtraMoveState() {
        Game g = gameInPhase(Phase.EXTRA_MOVE);
        ControllerState s = StateFactory.recover(g, makeController(g));
        assertInstanceOf(ExtraMoveState.class, s);
    }

    @Test
    void recover_EVENTS_EXECUTION_returnsEventsState() {
        Game g = gameInPhase(Phase.EVENTS_EXECUTION);
        ControllerState s = StateFactory.recover(g, makeController(g));
        assertInstanceOf(EventsState.class, s);
    }

    @Test
    void recover_END_ROUND_returnsEndRoundState() {
        Game g = gameInPhase(Phase.END_ROUND);
        ControllerState s = StateFactory.recover(g, makeController(g));
        assertInstanceOf(EndRoundState.class, s);
    }

    @Test
    void recover_END_GAME_returnsEndGameState() {
        Game g = gameInPhase(Phase.END_GAME);
        ControllerState s = StateFactory.recover(g, makeController(g));
        assertInstanceOf(EndGameState.class, s);
    }
}
