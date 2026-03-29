package org.example.server.controller.states;

import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.Board;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;
import org.example.server.model.game.FakeCard;
import org.example.shared.enums.Color;
import org.example.shared.exceptions.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EndRoundStateTest {

    private EndRoundState state;

    @BeforeEach
    void setUp() {
        // topRow with 0 tribe cards so refill loop does not run
        CardRow lowRow = new CardRow(3, 0);
        CardRow topRow = new CardRow(3, 0);
        OfferTrack offerTrack = new OfferTrack(new ArrayList<>());
        OrderTile orderTile = new OrderTile(new ArrayList<>());
        // Deck needs at least one card so isNewEra() doesn't throw
        Set<org.example.server.model.cards.Card> era1 = new HashSet<>();
        era1.add(new FakeCard());
        ArrayList<Set<org.example.server.model.cards.Card>> deckCards = new ArrayList<>();
        deckCards.add(era1);
        Board board = new Board(lowRow, topRow, offerTrack, orderTile, new ArrayList<>(), deckCards);
        Player p = new Player("P1", 10, 0, Color.RED);
        Set<Player> players = new HashSet<>();
        players.add(p);
        Game game = new Game(1, 1, players, p, board);
        state = new EndRoundState(game);
    }

    @Test
    void checkMove_alwaysThrowsInvalidMoveException() {
        assertThrows(InvalidRequestException.class,
                () -> state.checkMove(new HashSet<>(), null));
    }

    @Test
    void nextState_returnsTotemPlacementState() {
        assertInstanceOf(TotemPlacementState.class, state.nextState());
    }

    @Test
    void onEntry_sameEra_doesNotThrow() {
        // FakeCard has era = 1, game era = 1 → isNewEra returns false → no era change
        assertDoesNotThrow(state::onEntry);
    }
}
