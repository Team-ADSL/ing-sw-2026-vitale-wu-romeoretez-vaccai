package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.*;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class ExtraMoveStateTest {

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

        Board board = new Board(
                new CardRow(3, 3),
                new CardRow(6, 6),
                new OfferTrack(new ArrayList<>()),
                new OrderTile("order_tile_5p", new ArrayList<>()),
                new ArrayList<>(),
                new Deck(new ArrayList<>())
        );
        game.setBoard(board);

        GameController controller = new GameController(loader, new FakeGamePersistenceManager(), new FakeGameDAO());
        state = new ExtraMoveState(game, controller);
    }

    // ──────────────────────────────────────────────
    // TEST CALC NEXT STATE
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_noPlayerHasExtraMove_returnsEventsState() {
        ControllerState next = state.calcNextState();
        assertInstanceOf(EventsState.class, next);
    }

    @Test
    void testCalcNextState_playerHasExtraMove_returnsSelf() {
        p1.getBuildingBonus().setExtraMove(true);

        ControllerState next = state.calcNextState();
        assertInstanceOf(ExtraMoveState.class, next);
        assertSame(state, next);
    }

    @Test
    void testCalcNextState_playerHasExtraMove_setsCurrentPlayer() {
        p2.getBuildingBonus().setExtraMove(true);
        state.calcNextState();

        assertTrue(game.getCurrentPlayer().isPresent());
        assertEquals("p2", game.getCurrentPlayer().get().getName());
    }

    @Test
    void testCalcNextState_noExtraMove_clearsCurrentPlayer() {
        game.setCurrentPlayer(p1);
        state.calcNextState();
        assertFalse(game.getCurrentPlayer().isPresent());
    }
}
