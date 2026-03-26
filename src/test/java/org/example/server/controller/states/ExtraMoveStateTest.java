package org.example.server.controller.states;

import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.shared.enums.Color;
import org.example.shared.enums.Row;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.utils.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class ExtraMoveStateTest {

    private Player player;
    private ExtraMoveState state;

    @BeforeEach
    void setUp() {
        CardRow lowRow = new CardRow(3, 3);
        CardRow topRow = new CardRow(6, 0);
        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());
        OrderTile orderTile = new OrderTile(new ArrayList<>());
        Board board = new Board(lowRow, topRow, offerTrack, orderTile, new ArrayList<>(), new ArrayList<>());
        player = new Player("P1", 10, 0, Color.RED);
        Set<Player> players = new HashSet<>();
        players.add(player);
        Game game = new Game(1, 1, players, player, board);
        state = new ExtraMoveState(game);
    }

    @Test
    void checkMove_emptyMoves_throwsInvalidMoveException() {
        assertThrows(InvalidMoveException.class,
                () -> state.checkMove(new HashSet<>(), player));
    }

    @Test
    void checkMove_moreThanOneMove_throwsInvalidMoveException() {
        Set<Move> moves = new HashSet<>();
        moves.add(new Move(0, Row.UPPER));
        moves.add(new Move(1, Row.UPPER));
        assertThrows(InvalidMoveException.class,
                () -> state.checkMove(moves, player));
    }

    @Test
    void checkMove_moveFromLowerRow_throwsInvalidMoveException() {
        Set<Move> moves = new HashSet<>();
        moves.add(new Move(0, Row.LOWER));
        assertThrows(InvalidMoveException.class,
                () -> state.checkMove(moves, player));
    }

    @Test
    void checkMove_validUpperMove_doesNotThrow() {
        Set<Move> moves = new HashSet<>();
        moves.add(new Move(0, Row.UPPER));
        assertDoesNotThrow(() -> state.checkMove(moves, player));
    }

    @Test
    void nextState_returnsEventsState() {
        assertInstanceOf(EventsState.class, state.nextState());
    }
}
