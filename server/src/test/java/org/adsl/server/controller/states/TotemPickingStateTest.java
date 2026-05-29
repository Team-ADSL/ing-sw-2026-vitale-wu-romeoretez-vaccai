package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.Player;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.network.requests.TotemPickingRequest;
import org.adsl.utils.builder.GameControllerBuilder;
import org.adsl.utils.fakes.FakeGame;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TotemPickingStateTest {

    private FakeGame game;
    private TotemPickingState state;
    private Player p1;
    private Player p2;
    private FakeVirtualClient client1;
    private FakeVirtualClient client2;
    private GameController controller;

    @BeforeEach
    void setUp() {
        game = new FakeGame(1, 2);
        p1 = new Player("p1");
        p2 = new Player("p2");
        game.getPlayers().add(p1);
        game.getPlayers().add(p2);

        client1 = new FakeVirtualClient();
        client1.setClientUsername("p1");
        client2 = new FakeVirtualClient();
        client2.setClientUsername("p2");

        controller = new GameControllerBuilder().build();
        state = new TotemPickingState(game, controller);
        state.onEntry();
    }

    // ──────────────────────────────────────────────
    // TEST ON ENTRY
    // ──────────────────────────────────────────────

    @Test
    void testOnEntry_returnsSelf() throws ServerException {
        ControllerState next = state.onEntry();
        assertSame(state, next);
    }

    @Test
    void testOnEntry_sendsTotemsAvailable() throws ServerException {
        state.onEntry();
        assertTrue(game.sendTotemAvailableSent);
    }

    @Test
    void testOnEntry_sendsAllFiveTotems() throws ServerException {
        state.onEntry();
        assertEquals(Totem.values().length, game.lastTotemAvailable.size());
    }

    // ──────────────────────────────────────────────
    // TEST VISIT - SUCCESS
    // ──────────────────────────────────────────────

    @Test
    void testVisit_success_setsPlayerColor() throws ServerException {
        state.visit(new TotemPickingRequest(Totem.RED), client1);
        assertEquals(Totem.RED, p1.getColor());
    }

    @Test
    void testVisit_success_removesTotemFromAvailableList() throws ServerException {
        state.visit(new TotemPickingRequest(Totem.RED), client1);
        assertFalse(game.lastTotemAvailable.contains(Totem.RED));
    }

    @Test
    void testVisit_success_notifiesGame() throws ServerException {
        state.visit(new TotemPickingRequest(Totem.WHITE), client1);
        assertTrue(game.sendTotemAvailableSent);
    }

    @Test
    void testVisit_twoDifferentPlayers_bothColorsSet() throws ServerException {
        state.visit(new TotemPickingRequest(Totem.RED), client1);
        state.visit(new TotemPickingRequest(Totem.WHITE), client2);
        assertEquals(Totem.RED, p1.getColor());
        assertEquals(Totem.WHITE, p2.getColor());
    }

    // ──────────────────────────────────────────────
    // TEST VISIT - ERRORS
    // ──────────────────────────────────────────────

    @Test
    void testVisit_noUsername_throwsException() {
        FakeVirtualClient anonymous = new FakeVirtualClient();
        assertThrows(ServerException.class,
                () -> state.visit(new TotemPickingRequest(Totem.RED), anonymous));
    }

    @Test
    void testVisit_playerNotInGame_throwsException() {
        FakeVirtualClient outsider = new FakeVirtualClient();
        outsider.setClientUsername("stranger");
        assertThrows(ServerException.class,
                () -> state.visit(new TotemPickingRequest(Totem.RED), outsider));
    }

    @Test
    void testVisit_playerAlreadyPickedTotem_throwsException() {
        p1.setColor(Totem.RED);
        assertThrows(ServerException.class,
                () -> state.visit(new TotemPickingRequest(Totem.WHITE), client1));
    }

    @Test
    void testVisit_totemAlreadyTakenByOtherPlayer_throwsException() throws ServerException {
        state.visit(new TotemPickingRequest(Totem.RED), client1);
        assertThrows(ServerException.class,
                () -> state.visit(new TotemPickingRequest(Totem.RED), client2));
    }

    // ──────────────────────────────────────────────
    // TEST CALC NEXT STATE
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_noPicks_returnsSelf() {
        assertSame(state, state.calcNextState());
    }

    @Test
    void testCalcNextState_onlyOnePick_returnsSelf() throws ServerException {
        state.visit(new TotemPickingRequest(Totem.RED), client1);
        assertSame(state, state.calcNextState());
    }

    @Test
    void testCalcNextState_allPicked_returnsInitGameState() throws ServerException {
        state.visit(new TotemPickingRequest(Totem.RED), client1);
        state.visit(new TotemPickingRequest(Totem.WHITE), client2);
        assertInstanceOf(InitGameState.class, state.calcNextState());
    }

    // ──────────────────────────────────────────────
    // TEST DISCONNECTION
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_isToStop_returnsRecoverState() {
        state.setToStop(true);
        assertInstanceOf(RecoverState.class, state.calcNextState());
    }

    @Test
    void testVisitClientDisconnected_setsToStopAndTransitionsToRecoverState() throws ServerException {
        org.adsl.shared.network.requests.ClientDisconnected req =
                new org.adsl.shared.network.requests.ClientDisconnected();
        state.visit(req, client1);
        assertTrue(state.isToStop());
        assertInstanceOf(RecoverState.class, state.getNextState());
    }
}
