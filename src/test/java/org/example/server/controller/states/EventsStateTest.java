package org.example.server.controller.states;

import org.example.server.config.BoardConfigLoader;
import org.example.server.config.JsonBoardConfigLoader;
import org.example.server.controller.GameController;
import org.example.server.model.EndGameObserver;
import org.example.server.model.Game;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.server.persistence.GameDAO;
import org.example.server.persistence.GamePersistenceManager;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EventsStateTest {

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
        @Override public void saveMatch(int id, int count, List<String> n, List<Integer> s) {}
        @Override public List<MatchResult> getLeaderboard(int count) { return List.of(); }
    };

    private Game game;
    private EventsState eventsState;

    @BeforeEach
    void setUp() {
        game = new Game(1, NO_OP_END);
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);

        // Minimal board needed for onEntry (getLowRow().getTribeCards())
        Board board = new Board(
                new CardRow(3, 3),
                new CardRow(6, 6),
                new OfferTrack(new ArrayList<>()),
                new OrderTile(new ArrayList<>()),
                new ArrayList<>(),
                new ArrayList<>()
        );
        game.setBoard(board);

        eventsState = new EventsState(game, controller);
    }

    // ──────────────────────────────────────────────
    // nextState
    // ──────────────────────────────────────────────

    @Test
    void nextState_whenRoundIs10_returnsEndRoundState() {
        // Use recovery constructor to set round to 10
        Set<org.example.server.model.Player> players = new HashSet<>();
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        Game g = new Game(1, 10, 1, players, null,
                game.getBoard(), org.example.shared.enums.Phase.EVENTS_EXECUTION, true,
                NO_OP_END, NO_OP_PERSISTENCE);
        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        EventsState state = new EventsState(g, controller);

        assertInstanceOf(EndRoundState.class, state.nextState());
    }

    @Test
    void nextState_whenRoundIsNot10_returnsEndGameState() {
        // default round is 0, which is not 10
        assertInstanceOf(EndGameState.class, eventsState.nextState());
    }

    @Test
    void nextState_whenRoundIs5_returnsEndGameState() {
        Set<org.example.server.model.Player> players = new HashSet<>();
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        Game g = new Game(1, 5, 1, players, null,
                game.getBoard(), org.example.shared.enums.Phase.EVENTS_EXECUTION, true,
                NO_OP_END, NO_OP_PERSISTENCE);
        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        EventsState state = new EventsState(g, controller);

        assertInstanceOf(EndGameState.class, state.nextState());
    }
}
