package org.example.server.controller.states;

import org.example.server.config.BoardConfigLoader;
import org.example.server.config.JsonBoardConfigLoader;
import org.example.server.controller.GameController;
import org.example.server.model.EndGameObserver;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.*;
import org.example.server.persistence.GameDAO;
import org.example.server.persistence.GamePersistenceManager;
import org.example.shared.enums.Row;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.example.shared.utils.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TotemPlacementStateTest {

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

    private Game game;
    private TotemPlacementState state;
    private Player p1;
    private Player p2;
    private OfferTrack offerTrack;
    private OrderTile orderTile;

    /**
     * Build a minimal board for 2 players.
     * OfferTrack has 2 tiles (one per player), OrderTile has 2 cells.
     */
    @BeforeEach
    void setUp() {
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        game = new Game(1, NO_OP_END);

        p1 = new Player("p1");
        p2 = new Player("p2");
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        // 2-player offer track: 2 tiles, neither givesFood
        Map<Row, Integer> movesA = new EnumMap<>(Row.class);
        movesA.put(Row.UPPER, 1); movesA.put(Row.LOWER, 0);
        Map<Row, Integer> movesB = new EnumMap<>(Row.class);
        movesB.put(Row.UPPER, 0); movesB.put(Row.LOWER, 1);
        ArrayList<OfferTile> tiles = new ArrayList<>();
        tiles.add(new OfferTile(null, movesA, false));
        tiles.add(new OfferTile(null, movesB, false));
        offerTrack = new OfferTrack(tiles);

        // OrderTile: 2 cells (bonus=0, no malus)
        ArrayList<OrderCell> cells = new ArrayList<>();
        cells.add(new OrderCell(null, 0, false));
        cells.add(new OrderCell(null, 0, false));
        orderTile = new OrderTile(cells);

        Board board = new Board(
                new CardRow(3, 3),
                new CardRow(6, 6),
                offerTrack,
                orderTile,
                new ArrayList<>(),
                new ArrayList<>()
        );
        game.setBoard(board);

        GameController controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        state = new TotemPlacementState(game, controller);
    }

    // ──────────────────────────────────────────────
    // nextState — when not all totems placed
    // ──────────────────────────────────────────────

    @Test
    void nextState_returnsSelfWhenNotAllTotemsPlaced() {
        // Place p1 at index 0 of order tile: loop stops immediately (tile[0] not empty),
        // orderIndex = 0 != players.size()-1 (= 1) → returns this (TotemPlacementState)
        orderTile.placePlayerAtNext(p1);
        ControllerState next = state.nextState();
        assertInstanceOf(TotemPlacementState.class, next);
        assertSame(state, next);
    }

    // ──────────────────────────────────────────────
    // nextState — when all totems placed
    // ──────────────────────────────────────────────

    @Test
    void nextState_returnsActionExecutionStateWhenAllTotemsPlaced() {
        // For 2-player game: orderIndex == players.size()-1 == 1 triggers ActionExecutionState.
        // The while loop advances while isEmpty, so to reach index 1:
        //   tile[0] must be empty (loop increments), tile[1] must have a player (loop stops).
        // Manually place p2 at cell index 1 (set directly via getCellAt):
        orderTile.getCellAt(1).setPlayer(p2);

        ControllerState next = state.nextState();
        assertInstanceOf(ActionExecutionState.class, next);
    }

    // ──────────────────────────────────────────────
    // execute — places player in offer track
    // ──────────────────────────────────────────────

    @Test
    void execute_placesPlayerInOfferTrack() {
        // Tile at index 0 should be empty initially
        assertTrue(offerTrack.getTileAt(0).getPlayer().isEmpty());

        Move move = new Move(0, Row.UPPER);
        state.execute(move, p1);

        assertTrue(offerTrack.getTileAt(0).getPlayer().isPresent());
        assertSame(p1, offerTrack.getTileAt(0).getPlayer().get());
    }

    @Test
    void execute_placesPlayerAtChosenIndex() {
        Move move = new Move(1, Row.LOWER);
        state.execute(move, p2);

        assertTrue(offerTrack.getTileAt(1).getPlayer().isPresent());
        assertSame(p2, offerTrack.getTileAt(1).getPlayer().get());
    }
}
