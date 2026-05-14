package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.server.model.Player;
import org.adsl.shared.network.requests.EnterGameRequest;
import org.adsl.utils.builder.GameControllerBuilder;
import org.adsl.utils.fakes.FakeGame;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RecoverStateTest {
    private FakeVirtualClient client;
    private FakeGame fakeGame;
    private RecoverState state;
    private GameController controller;

    @BeforeEach
    void setUp() {
        client = new FakeVirtualClient();

        GameControllerBuilder builder = new GameControllerBuilder();
        controller = builder.build();

        fakeGame = new FakeGame(1, 2);
        state = new RecoverState(fakeGame, controller);
    }

    // ──────────────────────────────────────────────
    // TEST VISIT ENTER GAME REQUEST
    // ──────────────────────────────────────────────

    @Test
    void testVisitEnterGameRequest_clientHasNoUsername_throwsException() {
        EnterGameRequest req = new EnterGameRequest(1);

        assertThrows(ServerException.class, () -> state.visit(req, client));
    }

    @Test
    void testVisitEnterGameRequest_playerNotInGame_throwsException() {
        client.setClientUsername("Unknown");
        fakeGame.getPlayers().add(new Player("Alice"));
        EnterGameRequest req = new EnterGameRequest(1);

        ServerException exception = assertThrows(ServerException.class,
                () -> state.visit(req, client));

        assertTrue(exception.getMessage().contains("not in current game"));
    }

    @Test
    void testVisitEnterGameRequest_playerAlreadyActive_throwsException() {
        client.setClientUsername("Alice");
        Player alice = new Player("Alice");
        fakeGame.getPlayers().add(alice);
        EnterGameRequest req = new EnterGameRequest(1);

        ServerException exception = assertThrows(ServerException.class,
                () -> state.visit(req, client));

        assertTrue(exception.getMessage().contains("already connected"));
    }

    @Test
    void testVisitEnterGameRequest_inactivePlayer_reconnectsSuccessfully() throws ServerException {
        client.setClientUsername("Alice");
        Player alice = new Player("Alice");
        alice.setActive(false);
        fakeGame.getPlayers().add(alice);
        EnterGameRequest req = new EnterGameRequest(1);

        state.visit(req, client);

        assertTrue(alice.isActive(), "Player must be set active on reconnect");
        assertTrue(fakeGame.addedClients.contains(client), "Client must be added as observer");
        assertTrue(client.getGameId().isPresent(), "Client must have gameId set");
        assertTrue(fakeGame.updateLobbySent, "Lobby update must be sent after reconnection");
    }

    // ──────────────────────────────────────────────
    // TEST CALC NEXT STATE
    // ──────────────────────────────────────────────

    @Test
    void testCalcNextState_notAllPlayersActive_returnsSelf() {
        Player alice = new Player("Alice");
        alice.setActive(true);
        Player bob = new Player("Bob");
        bob.setActive(false);
        fakeGame.getPlayers().add(alice);
        fakeGame.getPlayers().add(bob);

        ControllerState next = state.calcNextState();

        assertSame(state, next, "State must remain RecoverState until all players reconnect");
    }

    @Test
    void testCalcNextState_allPlayersActive_returnsStateFromFactory() {
        org.adsl.shared.enums.Phase phase = org.adsl.shared.enums.Phase.TOTEM_PLACEMENT;
        fakeGame.setPhase(phase);
        Player alice = new Player("Alice");
        Player bob = new Player("Bob");
        fakeGame.getPlayers().add(alice);
        fakeGame.getPlayers().add(bob);

        ControllerState next = state.calcNextState();

        assertInstanceOf(TotemPlacementState.class, next,
                "When all players are active, must transition to the state matching the saved phase");
        assertTrue(fakeGame.updateGameSent, "Game update must be sent when all players are active");
    }
}
