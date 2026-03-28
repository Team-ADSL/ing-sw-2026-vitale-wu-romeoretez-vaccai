package org.example.server.controller.states;

import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.shared.enums.Color;
import org.example.shared.exceptions.InvalidMoveException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EventsControllerStateTest {

    private Game buildGame(int round) {
        CardRow lowRow = new CardRow(3, 3);
        CardRow topRow = new CardRow(6, 0);
        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());
        OrderTile orderTile = new OrderTile(new ArrayList<>());
        Board board = new Board(lowRow, topRow, offerTrack, orderTile, new ArrayList<>(), new ArrayList<>());
        Player p = new Player("P1", 10, 0, Color.RED);
        Set<Player> players = new HashSet<>();
        players.add(p);
        return new Game(round, 1, players, p, board);
    }

    @Test
    void checkMove_alwaysThrowsInvalidMoveException() {
        EventsControllerState state = new EventsControllerState(buildGame(1));
        assertThrows(InvalidMoveException.class,
                () -> state.checkMove(new HashSet<>(), null));
    }

    @Test
    void nextState_round10_returnsEndRoundState() {
        EventsControllerState state = new EventsControllerState(buildGame(10));
        assertInstanceOf(EndRoundControllerState.class, state.nextState());
    }

    @Test
    void nextState_roundNot10_returnsEndGameState() {
        EventsControllerState state = new EventsControllerState(buildGame(5));
        assertInstanceOf(EndGameControllerState.class, state.nextState());
    }

    @Test
    void onEntry_emptyLowRow_doesNotThrow() {
        // Empty low row (all null) — activeEffect is guarded by null check
        EventsControllerState state = new EventsControllerState(buildGame(5));
        assertDoesNotThrow(state::onEntry);
    }
}
