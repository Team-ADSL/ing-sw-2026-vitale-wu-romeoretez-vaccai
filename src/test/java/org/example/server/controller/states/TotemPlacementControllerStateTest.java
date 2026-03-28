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

public class TotemPlacementControllerStateTest {

    private Player player;
    private TotemPlacementControllerState state;

    @BeforeEach
    void setUp() {
        CardRow lowRow = new CardRow(3, 3);
        CardRow topRow = new CardRow(6, 0);
        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());

        // OrderTile with one cell pointing to our player
        player = new Player("P1", 10, 0, Color.RED);
        OrderCell cell = new OrderCell(Optional.of(player), 0, false);
        ArrayList<OrderCell> cells = new ArrayList<>();
        cells.add(cell);
        OrderTile orderTile = new OrderTile(cells);

        Board board = new Board(lowRow, topRow, offerTrack, orderTile, new ArrayList<>(), new ArrayList<>());
        Set<Player> players = new HashSet<>();
        players.add(player);
        Game game = new Game(1, 1, players, player, board);
        state = new TotemPlacementControllerState(game);
    }

    @Test
    void checkMove_emptyMoves_throwsInvalidMoveException() {
        assertThrows(InvalidMoveException.class,
                () -> state.checkMove(new HashSet<>(), player));
    }

    @Test
    void checkMove_wrongPlayer_throwsInvalidMoveException() {
        Player other = new Player("Other", 5, 0, Color.BLUE);
        Set<Move> moves = new HashSet<>();
        moves.add(new Move(0, Row.OFFER));
        assertThrows(InvalidMoveException.class,
                () -> state.checkMove(moves, other));
    }

    @Test
    void checkMove_correctPlayer_doesNotThrow() {
        Set<Move> moves = new HashSet<>();
        moves.add(new Move(0, Row.OFFER));
        assertDoesNotThrow(() -> state.checkMove(moves, player));
    }
}
