package org.example.server.controller.states;

import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.*;
import org.example.shared.enums.Color;
import org.example.shared.enums.Row;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.utils.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ActionExecutionStateTest {

    private Player player;
    private ActionExecutionState state;

    @BeforeEach
    void setUp() {
        CardRow lowRow = new CardRow(3, 3);
        CardRow topRow = new CardRow(6, 0);

        // OfferTrack with one tile that has a player and 1 move allowed (1 UPPER, 0 LOWER)
        player = new Player("P1", 10, 0, Color.RED);
        Map<Row, Integer> allowedMoves = new HashMap<>();
        allowedMoves.put(Row.UPPER, 1);
        allowedMoves.put(Row.LOWER, 0);
        OfferTile tile = new OfferTile(Optional.of(player), allowedMoves, false);
        ArrayList<OfferTile> tiles = new ArrayList<>();
        tiles.add(tile);
        OfferTrack offerTrack = new OfferTrack(tiles);

        OrderTile orderTile = new OrderTile(new ArrayList<>());
        Board board = new Board(lowRow, topRow, offerTrack, orderTile, new ArrayList<>(), new ArrayList<>());
        Set<Player> players = new HashSet<>();
        players.add(player);
        Game game = new Game(1, 1, players, player, board);
        state = new ActionExecutionState(game);
    }

    @Test
    void checkMove_wrongMoveCount_throwsInvalidMoveException() {
        // Offer tile allows 1 move, we provide 2
        Set<Move> moves = new HashSet<>();
        moves.add(new Move(0, Row.UPPER));
        moves.add(new Move(1, Row.UPPER));
        assertThrows(InvalidMoveException.class,
                () -> state.checkMove(moves, player));
    }

    @Test
    void checkMove_wrongRowDistribution_throwsInvalidMoveException() {
        // Offer tile allows 1 UPPER + 0 LOWER; we provide 0 UPPER + 1 LOWER
        Set<Move> moves = new HashSet<>();
        moves.add(new Move(0, Row.LOWER));
        assertThrows(InvalidMoveException.class,
                () -> state.checkMove(moves, player));
    }

    @Test
    void checkMove_correctMove_doesNotThrow() {
        Set<Move> moves = new HashSet<>();
        moves.add(new Move(0, Row.UPPER));
        assertDoesNotThrow(() -> state.checkMove(moves, player));
    }
}
