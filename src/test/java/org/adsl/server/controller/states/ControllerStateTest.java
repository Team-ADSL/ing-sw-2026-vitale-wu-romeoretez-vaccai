package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.exceptions.ServerException;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.shared.network.requests.*;
import org.adsl.utils.builder.GameControllerBuilder;
import org.adsl.utils.fakes.FakeGame;
import org.adsl.utils.fakes.FakePlayer;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ControllerStateTest {

    private static class ConcreteControllerState extends ControllerState {
        public ConcreteControllerState(Game game, GameController context) {
            super(game, context);
        }
    }

    private FakeVirtualClient client;
    private FakeGame fakeGame;
    private ConcreteControllerState state;
    private FakePlayer player1;

    @BeforeEach
    void setUp() {
        client = new FakeVirtualClient();
        client.setClientUsername("Player1");

        GameControllerBuilder builder = new GameControllerBuilder();
        GameController controller = builder.build();

        player1 = new FakePlayer("Player1");
        FakePlayer player2 = new FakePlayer("Player2");

        fakeGame = new FakeGame();
        fakeGame.players.add(player1);
        fakeGame.players.add(player2);
        fakeGame.currentPlayer = player1;

        state = new ConcreteControllerState(fakeGame, controller);
    }

    // ──────────────────────────────────────────────
    // TEST DEFAULT STATE BEHAVIORS
    // ──────────────────────────────────────────────

    @Test
    void testOnEntryAndCalcNextState_returnSelfByDefault() throws ServerException {
        assertEquals(state, state.onEntry());
        assertEquals(state, state.calcNextState());
    }

    // ──────────────────────────────────────────────
    // TEST TURN CONTROL
    // ──────────────────────────────────────────────

    @Test
    void testControlIfPlayerTurn_noUsername_throwsException() {
        client.setClientUsername(null);

        ServerException exception = assertThrows(ServerException.class,
                () -> state.controlIfPlayerTurn(client));

        assertTrue(exception.getMessage().contains("no username"));
    }

    @Test
    void testControlIfPlayerTurn_playerNotInGame_throwsException() {
        client.setClientUsername("GhostPlayer");

        ServerException exception = assertThrows(ServerException.class,
                () -> state.controlIfPlayerTurn(client));

        assertTrue(exception.getMessage().contains("not in current game"));
    }

    @Test
    void testControlIfPlayerTurn_notCurrentPlayer_throwsException() {
        client.setClientUsername("Player2");

        ServerException exception = assertThrows(ServerException.class,
                () -> state.controlIfPlayerTurn(client));

        assertTrue(exception.getMessage().contains("current player is"));
    }

    @Test
    void testControlIfPlayerTurn_success_returnsPlayer() throws ServerException {
        Player result = state.controlIfPlayerTurn(client);

        assertEquals("Player1", result.getName());
    }

    // ──────────────────────────────────────────────
    // TEST REQUEST REJECTIONS
    // ──────────────────────────────────────────────

    @Test
    void testVisitMethods_defaultImplementations_rejectUnallowedRequests() {
        assertThrows(ServerException.class, () -> state.visit(new ClientConnection(), client));
        assertThrows(ServerException.class, () -> state.visit(new LoginRequest("P1"), client));
        assertThrows(ServerException.class, () -> state.visit(new CreateGameRequest(2), client));
        assertThrows(ServerException.class, () -> state.visit(new EnterGameRequest(1), client));
        assertThrows(ServerException.class, () -> state.visit(new StartGameRequest(), client));
        assertThrows(ServerException.class, () -> state.visit(new MoveRequest(null), client));
    }

    // ──────────────────────────────────────────────
    // TEST DISCONNECTION
    // ──────────────────────────────────────────────

    @Test
    void testVisitClientDisconnected_setsPlayerInactiveAndRemovesClient() throws ServerException {
        ClientDisconnected req = new ClientDisconnected();

        state.visit(req, client);

        assertFalse(player1.isActive());
        assertTrue(fakeGame.clientRemoved);
        assertTrue(state.isToStop());
    }
}