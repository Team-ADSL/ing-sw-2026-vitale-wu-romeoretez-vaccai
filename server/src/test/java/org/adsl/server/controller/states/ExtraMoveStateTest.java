package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.*;
import org.adsl.shared.enums.Row;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.network.requests.MoveRequest;
import org.adsl.shared.utils.Move;
import org.adsl.utils.fakes.FakeCard;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Set;

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

    // ──────────────────────────────────────────────
    // TEST ON ENTRY
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_noExtraMove_returnsEventsState() {
        ControllerState next = state.onEntry();
        assertInstanceOf(EventsState.class, next);
    }

    @Test
    void testOnEntry_playerHasExtraMove_returnsSelf() {
        p1.getBuildingBonus().setExtraMove(true);
        ControllerState next = state.onEntry();
        assertSame(state, next);
    }

    // ──────────────────────────────────────────────
    // TEST VISIT - ERRORS
    // ──────────────────────────────────────────────

    @Test
    void testVisit_moreThanOneMove_throwsException() {
        game.setCurrentPlayer(p1);
        FakeVirtualClient client = new FakeVirtualClient();
        client.setClientUsername("p1");
        MoveRequest req = new MoveRequest(Set.of(new Move(0, Row.UPPER), new Move(1, Row.UPPER)));
        assertThrows(ServerException.class, () -> state.visit(req, client));
    }

    @Test
    void testVisit_lowerRowMove_throwsException() {
        game.setCurrentPlayer(p1);
        FakeVirtualClient client = new FakeVirtualClient();
        client.setClientUsername("p1");
        game.getBoard().topRow().add(new FakeCard());
        MoveRequest req = new MoveRequest(Set.of(new Move(0, Row.LOWER)));
        assertThrows(ServerException.class, () -> state.visit(req, client));
    }

    @Test
    void testVisit_emptyMoves_setsNextStateToEventsState() throws ServerException {
        game.setCurrentPlayer(p1);
        FakeVirtualClient client = new FakeVirtualClient();
        client.setClientUsername("p1");
        MoveRequest req = new MoveRequest(Set.of());
        state.visit(req, client);
        assertInstanceOf(EventsState.class, state.getNextState());
    }

    // ──────────────────────────────────────────────
    // TEST EXECUTE
    // ──────────────────────────────────────────────

    @Test
    void testExecute_picksCardFromTopRow() {
        FakeCard card = new FakeCard();
        game.getBoard().topRow().add(card);
        Move move = new Move(0, Row.UPPER);
        state.execute(move, p1);
        assertNull(game.getBoard().topRow().getCardAt(0));
    }

    @Test
    void testExecute_setsNextStateToEventsState() {
        game.getBoard().topRow().add(new FakeCard());
        Move move = new Move(0, Row.UPPER);
        state.execute(move, p1);
        assertInstanceOf(EventsState.class, state.getNextState());
    }
}
