package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.Player;
import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.network.requests.ClientDisconnected;
import org.adsl.shared.network.requests.EnterGameRequest;
import org.adsl.shared.network.requests.StartGameRequest;

import org.adsl.utils.builder.GameControllerBuilder;
import org.adsl.utils.fakes.FakeGame;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyStateTest {
    private FakeVirtualClient client;
    private FakeGame fakeGame;
    private LobbyState state;

    @BeforeEach
    void setUp() {
        client = new FakeVirtualClient();
        client.setClientUsername("Player1");

        GameControllerBuilder builder = new GameControllerBuilder();
        GameController controller = builder.build();

        fakeGame = new FakeGame(1, 4);
        state = new LobbyState(fakeGame, controller, "Player1");
    }

    // ──────────────────────────────────────────────
    // TEST ENTER GAME
    // ──────────────────────────────────────────────

    @Test
    void testVisitEnterGameRequest_success_addsPlayerAndUpdatesLobby() throws ServerException {
        EnterGameRequest req = new EnterGameRequest(1);

        state.visit(req, client);

        assertEquals(1, fakeGame.getPlayers().size());
        boolean playerExists = fakeGame.getPlayers().stream()
                .anyMatch(p -> p.getName().equals("Player1"));
        assertTrue(playerExists, "Player1 must be successfully added to the game Set");
        assertTrue(fakeGame.addedClients.contains(client), "Client should be added to game observers");
        assertTrue(client.getGameId().isPresent(), "Client should have the gameId set");
        assertTrue(fakeGame.updateLobbySent, "Game should notify clients of the new player");
        assertEquals(state, state.calcNextState(), "State should remain LobbyState");
    }

    @Test
    void testVisitEnterGameRequest_lobbyFull_throwsException() {
        fakeGame.getPlayers().add(new Player("P1"));
        fakeGame.getPlayers().add(new Player("P2"));
        fakeGame.getPlayers().add(new Player("P3"));
        fakeGame.getPlayers().add(new Player("P4"));
        EnterGameRequest req = new EnterGameRequest(1);

        ServerException exception = assertThrows(ServerException.class,
                () -> state.visit(req, client));

        assertTrue(exception.getMessage().contains("lobby is full"));
    }

    @Test
    void testVisitEnterGameRequest_duplicatePlayer_throwsException() {
        fakeGame.getPlayers().add(new Player("Player1"));
        EnterGameRequest req = new EnterGameRequest(1);

        ServerException exception = assertThrows(ServerException.class,
                () -> state.visit(req, client));

        assertTrue(exception.getMessage().contains("already"),
                "Must prevent the same player from joining multiple times");
    }

    // ──────────────────────────────────────────────
    // TEST DISCONNECTION
    // ──────────────────────────────────────────────

    @Test
    void testVisitClientDisconnected_removesPlayerAndUpdatesLobby() throws ServerException {
        client.setClientUsername("Player2"); // Otherwise trigger host disconnection

        fakeGame.getPlayers().add(new Player("Player1"));
        fakeGame.getPlayers().add(new Player("Player2"));
        fakeGame.addedClients.add(client);
        client.setGameId(1);

        ClientDisconnected req = new ClientDisconnected();

        state.visit(req, client);

        assertEquals(1, fakeGame.getPlayers().size());
        boolean playerExists = fakeGame.getPlayers().stream()
                .anyMatch(p -> p.getName().equals("Player1"));
        assertTrue(playerExists, "Player1 must be successfully added to the game Set");
        assertTrue(fakeGame.removedClients.contains(client), "Client must be removed from observers");
        assertTrue(client.getGameId().isEmpty(), "Client gameId must be cleared");
        assertTrue(fakeGame.updateLobbySent, "Game should notify remaining clients");
        assertFalse(state.isToStop(), "State machine MUST NOT stop for a lobby disconnection");
    }

    @Test
    void testVisitClientDisconnected_lastPlayer_triggersEndGameResults() throws ServerException {
        fakeGame.getPlayers().add(new Player("Player1"));
        fakeGame.addedClients.add(client);
        ClientDisconnected req = new ClientDisconnected();

        state.visit(req, client);

        assertTrue(fakeGame.getPlayers().isEmpty());
        assertTrue(fakeGame.endGameResultsSent, "If lobby is empty, trigger the ServerController cleanup");
    }

    // ──────────────────────────────────────────────
    // TEST START GAME
    // ──────────────────────────────────────────────

    @Test
    void testVisitStartGameRequest_notFull_throwsException() {
        fakeGame.getPlayers().add(new Player("P1"));
        StartGameRequest req = new StartGameRequest();

        ServerException exception = assertThrows(ServerException.class,
                () -> state.visit(req, client));

        assertTrue(exception.getMessage().contains("players required"));
    }

    @Test
    void testVisitStartGameRequest_full_transitionsToInitState() throws ServerException {
        fakeGame.getPlayers().add(new Player("P1"));
        fakeGame.getPlayers().add(new Player("P2"));
        fakeGame.getPlayers().add(new Player("P3"));
        fakeGame.getPlayers().add(new Player("P4"));
        StartGameRequest req = new StartGameRequest();

        state.visit(req, client);
        ControllerState nextState = state.calcNextState();

        assertInstanceOf(InitGameState.class, nextState, "The state machine must transition to InitGameState when start is valid");
    }
}
