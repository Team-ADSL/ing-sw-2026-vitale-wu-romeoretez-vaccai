package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.controller.ServerController;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.*;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.Row;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.network.requests.MoveRequest;
import org.adsl.shared.network.responses.ServerResponse;
import org.adsl.shared.utils.Move;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.adsl.utils.fakes.FakeHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ActionExecutionStateTest {

    static class TestVirtualClient extends VirtualClient {
        TestVirtualClient(ServerController sc, String username) {
            super(sc);
            setClientUsername(username);
        }
        @Override
        public void sendResponse(ServerResponse response) {}
        @Override
        public void closeConnection() {}
    }

    private Game game;
    private ActionExecutionState state;
    private Player p1;
    private Player p2;
    private OfferTrack offerTrack;
    private OrderTile orderTile;
    private ServerController serverController;
    private GameController controller;

    @BeforeEach
    void setUp() {
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        FakeGameDAO fakeGameDAO = new FakeGameDAO();
        FakeGamePersistenceManager fakePersistence = new FakeGamePersistenceManager();
        serverController = new ServerController(new FakeHome(), fakeGameDAO, loader, fakePersistence, null, null, null);

        game = new Game(1, 5);
        p1 = new Player("p1");
        p2 = new Player("p2");
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        Map<Row, Integer> movesA = new EnumMap<>(Row.class);
        movesA.put(Row.UPPER, 1); movesA.put(Row.LOWER, 0);
        Map<Row, Integer> movesB = new EnumMap<>(Row.class);
        movesB.put(Row.UPPER, 0); movesB.put(Row.LOWER, 0);

        ArrayList<OfferTile> tiles = new ArrayList<>();
        tiles.add(new OfferTile("offer_tile_2p", p1, movesA, false));
        tiles.add(new OfferTile("offer_tile_2p", null, movesB, false));
        offerTrack = new OfferTrack(tiles);

        ArrayList<OrderCell> cells = new ArrayList<>();
        cells.add(new OrderCell(null, 0, false));
        cells.add(new OrderCell(null, 0, false));
        orderTile = new OrderTile("order_tile_2p", cells);

        Board board = new Board(
                new CardRow(3, 3),
                new CardRow(6, 6),
                offerTrack,
                orderTile,
                new ArrayList<>(),
                new Deck(new ArrayList<>())
        );
        game.setBoard(board);
        game.setCurrentPlayer(p1);

        controller = new GameController(loader, fakePersistence, fakeGameDAO);
        state = new ActionExecutionState(game, controller);
    }

    // ──────────────────────────────────────────────
    // TEST CALC NEXT STATE
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_playerInOfferTrack_returnsSelf() {
        ControllerState next = state.calcNextState();
        assertInstanceOf(ActionExecutionState.class, next);
        assertSame(state, next);
    }

    @Test
    void testCalcNextState_firstTileEmptySecondHasPlayer_staysActionExecution() {
        offerTrack.getTileAt(0).setPlayer(null);
        offerTrack.getTileAt(1).setPlayer(p2);
        game.setCurrentPlayer(p2);

        ControllerState next = state.calcNextState();
        assertInstanceOf(ActionExecutionState.class, next);
    }

    @Test
    void testCalcNextState_givesFoodTilePlayerStillAccessible() {
        assertTrue(offerTrack.getTileAt(0).getPlayer().isPresent());
        assertEquals("p1", offerTrack.getTileAt(0).getPlayer().get().getName());
    }

    // ──────────────────────────────────────────────
    // TEST VISIT MOVE REQUEST
    // ──────────────────────────────────────────────

    @Test
    void testVisitMoveRequest_wrongMoveCount_throwsException() {
        TestVirtualClient client = new TestVirtualClient(serverController, "p1");
        Set<Move> twoMoves = Set.of(
                new Move(0, Row.UPPER),
                new Move(1, Row.UPPER)
        );
        MoveRequest req = new MoveRequest(twoMoves);
        assertThrows(ServerException.class, () -> state.visit(req, client));
    }

    @Test
    void testVisitMoveRequest_notPlayerTurn_throwsException() {
        TestVirtualClient client = new TestVirtualClient(serverController, "p2");
        Set<Move> moves = Set.of(new Move(0, Row.UPPER));
        MoveRequest req = new MoveRequest(moves);
        assertThrows(ServerException.class, () -> state.visit(req, client));
    }
}
