package org.example.server.controller.states;

import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.shared.enums.Color;
import org.example.shared.exceptions.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EndGameStateTest {

    private Player player;
    private EndGameState state;

    @BeforeEach
    void setUp() {
        CardRow lowRow = new CardRow(3, 3);
        CardRow topRow = new CardRow(6, 0);
        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());
        OrderTile orderTile = new OrderTile(new ArrayList<>());
        Board board = new Board(lowRow, topRow, offerTrack, orderTile, new ArrayList<>(), new ArrayList<>());
        player = new Player("P1", 5, 0, Color.RED);
        Set<Player> players = new HashSet<>();
        players.add(player);
        Game game = new Game(1, 1, players, player, board);
        state = new EndGameState(game);
    }

    @Test
    void checkMove_alwaysThrowsInvalidMoveException() {
        assertThrows(InvalidRequestException.class,
                () -> state.checkMove(new HashSet<>(), player));
    }

    @Test
    void nextState_returnsNull() {
        assertNull(state.nextState());
    }

    @Test
    void onEntry_playerWithNoCards_ppUnchanged() {
        int initialPP = player.getPp();
        state.onEntry();
        assertEquals(initialPP, player.getPp());
    }
}
