package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.board.*;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EventsStateTest {
    private static final GamePersistenceManager NO_OP_PERSISTENCE = new GamePersistenceManager() {
        @Override public List<Game> recoverGames() { return List.of(); }
        @Override public void removeGame(int id) {}
        @Override public void updateLobby(List<String> p) {}
        @Override public void updateGame(Game g) {}
    };
    private static final GameDAO NO_OP_DAO = new GameDAO() {
        @Override public int createMatch() { return 0; }
        @Override public void deleteMatch(int id) {}
        @Override public void saveMatch(int id, int count, List<String> n, List<Integer> s) {}
        @Override public List<MatchResult> getLeaderboard(int count) { return List.of(); }
    };

    private Game game;
    private EventsState eventsState;

    @BeforeEach
    void setUp() {
        game = new Game(1, 5);
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);

        // Minimal board needed for onEntry (getLowRow().getTribeCards())
        Board board = new Board(
                new CardRow(3, 3),
                new CardRow(6, 6),
                new OfferTrack(new ArrayList<>()),
                new OrderTile("order_tile_5p", new ArrayList<>()),
                new ArrayList<>(),
                new Deck(new ArrayList<>())
        );
        game.setBoard(board);

        eventsState = new EventsState(game, controller);
    }

    // ──────────────────────────────────────────────
    // nextState
    // ──────────────────────────────────────────────

    @Test
    void nextState_whenRoundIs10_returnsEndGameState() {
        // Use recovery constructor to set round to 10
        Set<org.adsl.server.model.Player> players = new HashSet<>();
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        Game g = new Game(1, 5, 10, 1, players, null,
                game.getBoard(), org.adsl.shared.enums.Phase.EVENTS_EXECUTION);
        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        EventsState state = new EventsState(g, controller);

        assertInstanceOf(EndGameState.class, state.calcNextState());
    }

    @Test
    void nextState_whenRoundIsNot10_returnsEndRoundState() {
        assertInstanceOf(EndRoundState.class, eventsState.calcNextState());
    }
}
