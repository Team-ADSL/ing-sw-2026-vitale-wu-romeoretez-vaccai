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
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TotemPlacementStateTest {

    private Game game;
    private TotemPlacementState state;
    private Player p1;
    private Player p2;
    private OfferTrack offerTrack;
    private OrderTile orderTile;

    @BeforeEach
    void setUp() {
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        game = new Game(1, 2);

        p1 = new Player("p1");
        p2 = new Player("p2");
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        Map<Row, Integer> movesA = new EnumMap<>(Row.class);
        movesA.put(Row.UPPER, 1); movesA.put(Row.LOWER, 0);
        Map<Row, Integer> movesB = new EnumMap<>(Row.class);
        movesB.put(Row.UPPER, 0); movesB.put(Row.LOWER, 1);
        ArrayList<OfferTile> tiles = new ArrayList<>();
        tiles.add(new OfferTile("offer_tile_2p", null, movesA, false));
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

        GameController controller = new GameController(loader, new FakeGamePersistenceManager(), new FakeGameDAO());
        state = new TotemPlacementState(game, controller);
    }

    // ──────────────────────────────────────────────
    // TEST CALC NEXT STATE
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_notAllTotemsPlaced_returnsSelf() {
        orderTile.placePlayerAtNext(p1);
        ControllerState next = state.calcNextState();
        assertInstanceOf(TotemPlacementState.class, next);
        assertSame(state, next);
    }

    @Test
    void testCalcNextState_allTotemsPlaced_returnsActionExecutionState() {
        orderTile.removePlayer(p1);
        orderTile.removePlayer(p2);

        ControllerState next = state.calcNextState();
        assertInstanceOf(ActionExecutionState.class, next);
    }

    // ──────────────────────────────────────────────
    // TEST EXECUTE
    // ──────────────────────────────────────────────

    @Test
    void testExecute_placesPlayerInOfferTrack() {
        assertTrue(offerTrack.getTileAt(0).getPlayer().isEmpty());

        Move move = new Move(0, Row.UPPER);
        state.execute(move, p1);

        assertTrue(offerTrack.getTileAt(0).getPlayer().isPresent());
        assertSame(p1, offerTrack.getTileAt(0).getPlayer().get());
    }

    @Test
    void testExecute_placesPlayerAtChosenIndex() {
        Move move = new Move(1, Row.LOWER);
        state.execute(move, p2);

        assertTrue(offerTrack.getTileAt(1).getPlayer().isPresent());
        assertSame(p2, offerTrack.getTileAt(1).getPlayer().get());
    }

    // ──────────────────────────────────────────────
    // TEST ON ENTRY
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_currentPlayerAlreadySet_returnsSelf() throws ServerException {
        game.setCurrentPlayer(p1);
        ControllerState next = state.onEntry();
        assertSame(state, next);
    }

    @Test
    void testOnEntry_noCurrentPlayer_setsFirstFromOrderTile() throws ServerException {
        orderTile.placePlayerAtNext(p1);
        game.setCurrentPlayer(null);
        state.onEntry();
        assertTrue(game.getCurrentPlayer().isPresent());
        assertSame(p1, game.getCurrentPlayer().get());
    }

    @Test
    void testOnEntry_noCurrentPlayer_returnsSelf() throws ServerException {
        game.setCurrentPlayer(null);
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
        MoveRequest req = new MoveRequest(Set.of(
                new Move(0, Row.OFFER), new Move(1, Row.OFFER)));
        assertThrows(ServerException.class, () -> state.visit(req, client));
    }

    @Test
    void testVisit_wrongRow_throwsException() {
        game.setCurrentPlayer(p1);
        FakeVirtualClient client = new FakeVirtualClient();
        client.setClientUsername("p1");
        MoveRequest req = new MoveRequest(Set.of(new Move(0, Row.UPPER)));
        assertThrows(ServerException.class, () -> state.visit(req, client));
    }

    @Test
    void testVisit_outOfBoundsIndex_throwsException() {
        game.setCurrentPlayer(p1);
        FakeVirtualClient client = new FakeVirtualClient();
        client.setClientUsername("p1");
        MoveRequest req = new MoveRequest(Set.of(new Move(99, Row.OFFER)));
        assertThrows(ServerException.class, () -> state.visit(req, client));
    }

    @Test
    void testVisit_occupiedTile_throwsException() {
        game.setCurrentPlayer(p1);
        offerTrack.getTileAt(0).setPlayer(p2);
        FakeVirtualClient client = new FakeVirtualClient();
        client.setClientUsername("p1");
        MoveRequest req = new MoveRequest(Set.of(new Move(0, Row.OFFER)));
        assertThrows(ServerException.class, () -> state.visit(req, client));
    }
}
