package org.adsl.server.controller.states;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.*;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Trigger;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class EndRoundStateTest {

    private static Card fakeCard(int era) {
        return new Card("fake_" + era, era, null) {
            @Override public boolean canBeDrawn(Player p) { return true; }
            @Override public void insert(Map<CardType, Set<Card>> cards) {}
            @Override public void activeEffect(Set<Player> players, Trigger t) {}
            @Override
            public int getCost() {
                return 0;
            }
            @Override protected String getTypeLabel() { return "fake"; }
            @Override protected String getEffectsLabel() { return ""; }
        };
    }

    private Game game;
    private EndRoundState endRoundState;
    private BoardConfigLoader loader;

    @BeforeEach
    void setUp() {
        loader = new JsonBoardConfigLoader();
        game = new Game(1, 5);

        Set<Card> era1 = new HashSet<>();
        for (int i = 0; i < 15; i++) era1.add(fakeCard(1));
        ArrayList<Set<Card>> deckCards = new ArrayList<>(List.of(era1));

        Deck deck = Deck.createDeck(deckCards);
        Board board = new Board(3, 3, 6, 6,
                new OfferTrack(new ArrayList<>()),
                new OrderTile("order_tile_5p", new ArrayList<>()),
                deck);
        game.setBoard(board);

        GameController controller = new GameController(loader, new FakeGamePersistenceManager(), new FakeGameDAO());
        endRoundState = new EndRoundState(game, controller);
    }

    // ──────────────────────────────────────────────
    // TEST CALC NEXT STATE
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_defaultState_returnsTotemPlacementState() {
        assertInstanceOf(TotemPlacementState.class, endRoundState.calcNextState());
    }

    // ──────────────────────────────────────────────
    // TEST ON ENTRY
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_setsPhaseToTotemPlacement() throws Exception {
        endRoundState.onEntry();
        assertEquals(Phase.TOTEM_PLACEMENT, game.getPhase());
    }

    @Test
    void testOnEntry_incrementsRound() throws Exception {
        int before = game.getRound();
        endRoundState.onEntry();
        assertEquals(before + 1, game.getRound());
    }

    @Test
    void testOnEntry_topRowTribeCardsClearedAfterMove() throws Exception {
        CardRow topRow = game.getBoard().topRow();
        topRow.add(fakeCard(1));
        topRow.add(fakeCard(1));

        endRoundState.onEntry();

        assertEquals(2, game.getRound());
    }

    @Test
    void testOnEntry_lowRowReceivesCardsFromTopRow() throws Exception {
        Card sentinel = fakeCard(1);
        game.getBoard().topRow().add(sentinel);

        endRoundState.onEntry();

        ArrayList<Card> lowTribe = game.getBoard().lowRow().getTribeCards();
        boolean found = false;
        for (Card c : lowTribe) {
            if (c == sentinel) { found = true; break; }
        }
        assertTrue(found, "Sentinel card from top row should appear in low row after EndRound");
    }
}
