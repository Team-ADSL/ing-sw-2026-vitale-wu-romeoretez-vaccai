package org.example.server.controller;

import org.example.server.controller.states.ControllerState;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.shared.enums.Color;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.utils.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GameControllerTest {

    // Spy state: tracks whether transition was called and returns itself
    private static class SpyControllerState extends ControllerState {
        boolean transitionCalled = false;

        public SpyControllerState(Game game) {
            super(game);
        }

        @Override
        public ControllerState transition(Set<Move> moves, Player p) {
            transitionCalled = true;
            return this;
        }

        @Override
        public void checkMove(Set<Move> moves, Player p) throws InvalidMoveException {}

        @Override
        public void execute(Set<Move> moves, Player p) {}

        @Override
        public ControllerState nextState() { return this; }

        @Override
        public ControllerState onEntry() { return this; }
    }

    private Player player;
    private SpyControllerState spyState;
    private GameController controller;

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

        spyState = new SpyControllerState(game);
        controller = new GameController(1, spyState);
    }

    @Test
    void handleMoveRequest_callsTransitionOnState() {
        controller.handleMoveRequest(new HashSet<>(), player);
        assertTrue(spyState.transitionCalled);
    }

    @Test
    void handleMoveRequest_doesNotThrow() {
        assertDoesNotThrow(() -> controller.handleMoveRequest(new HashSet<>(), player));
    }
}
