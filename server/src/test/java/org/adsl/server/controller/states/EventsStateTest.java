package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.board.*;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EventsStateTest {

    private Game game;
    private EventsState eventsState;

    @BeforeEach
    void setUp() {
        game = new Game(1, 5);
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        GameController controller = new GameController(loader, new FakeGamePersistenceManager(), new FakeGameDAO());

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
    // TEST CALC NEXT STATE
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_round10_returnsEndGameState() {
        Set<org.adsl.server.model.Player> players = new HashSet<>();
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        Game g = new Game(1, 5, 10, 1, players, null,
                game.getBoard(), org.adsl.shared.enums.Phase.EVENTS_EXECUTION);
        GameController controller = new GameController(loader, new FakeGamePersistenceManager(), new FakeGameDAO());
        EventsState state = new EventsState(g, controller);

        assertInstanceOf(EndGameState.class, state.calcNextState());
    }

    @Test
    void testCalcNextState_roundNot10_returnsEndRoundState() {
        assertInstanceOf(EndRoundState.class, eventsState.calcNextState());
    }
}
