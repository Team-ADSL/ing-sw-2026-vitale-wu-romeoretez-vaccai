package org.adsl.server.controller;

import org.adsl.server.controller.states.*;
import org.adsl.server.model.Game;
import org.adsl.shared.enums.Phase;
import org.adsl.utils.builder.GameControllerBuilder;
import org.adsl.utils.fakes.FakeGame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StateFactoryTest {
    private GameController controller;

    @BeforeEach
    void setUp() {
        controller = new GameControllerBuilder().build();
    }

    // ──────────────────────────────────────────────
    // PHASE → STATE MAPPINGS
    // ──────────────────────────────────────────────

    @Test
    void testRecover_totemPlacementPhase_returnsTotemPlacementState() {
        FakeGame game = new FakeGame(1, 2);
        game.setPhase(Phase.TOTEM_PLACEMENT);

        ControllerState state = StateFactory.recover(game, controller);

        assertInstanceOf(TotemPlacementState.class, state);
    }

    @Test
    void testRecover_actionExecutionPhase_returnsActionExecutionState() {
        FakeGame game = new FakeGame(1, 2);
        game.setPhase(Phase.ACTION_EXECUTION);

        ControllerState state = StateFactory.recover(game, controller);

        assertInstanceOf(ActionExecutionState.class, state);
    }

    @Test
    void testRecover_extraMovePhase_returnsExtraMoveState() {
        FakeGame game = new FakeGame(1, 2);
        game.setPhase(Phase.EXTRA_MOVE);

        ControllerState state = StateFactory.recover(game, controller);

        assertInstanceOf(ExtraMoveState.class, state);
    }

    @Test
    void testRecover_eventsExecutionPhase_returnsEventsState() {
        FakeGame game = new FakeGame(1, 2);
        game.setPhase(Phase.EVENTS_EXECUTION);

        ControllerState state = StateFactory.recover(game, controller);

        assertInstanceOf(EventsState.class, state);
    }

    @Test
    void testRecover_endRoundPhase_returnsEndRoundState() {
        FakeGame game = new FakeGame(1, 2);
        game.setPhase(Phase.END_ROUND);

        ControllerState state = StateFactory.recover(game, controller);

        assertInstanceOf(EndRoundState.class, state);
    }

    @Test
    void testRecover_endGamePhase_returnsEndGameState() {
        FakeGame game = new FakeGame(1, 2);
        game.setPhase(Phase.END_GAME);

        ControllerState state = StateFactory.recover(game, controller);

        assertInstanceOf(EndGameState.class, state);
    }

    @Test
    void testRecover_nullPhase_throwsNullPointerException() {
        Game game = new Game(1, 2);

        assertThrows(NullPointerException.class, () -> StateFactory.recover(game, controller));
    }
}
