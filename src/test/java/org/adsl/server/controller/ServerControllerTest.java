package org.adsl.server.controller;

import org.adsl.TestDummies;
import org.adsl.fakes.FakeGameDAO;
import org.adsl.fakes.FakeGamePersistenceManager;
import org.adsl.fakes.FakeRegistry;
import org.adsl.fakes.FakeVirtualClient;
import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.exceptions.ServerException;
import org.adsl.server.network.socket.SocketServer;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.requests.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.rmi.registry.Registry;

import static org.junit.jupiter.api.Assertions.*;

public class ServerControllerTest {
    private FakeGameDAO fakeGameDAO;
    private FakeGamePersistenceManager fakePersistence;

    private FakeVirtualClient client;
    private ServerController serverController;

    @BeforeEach
    void setUp() {
        fakeGameDAO = new FakeGameDAO();
        client = new FakeVirtualClient();
        fakePersistence = new FakeGamePersistenceManager();
        Registry fakeRegistry = new FakeRegistry();

        BoardConfigLoader fakeBoardConfig = new TestDummies.DummyBoardConfigLoader();
        RemoteServerService fakeRmiServer = new TestDummies.DummyRemoteServerService();
        SocketServer fakeSocketServer = new TestDummies.DummySocketServer();

        serverController = new ServerController(
                fakeGameDAO, fakeBoardConfig, fakePersistence,
                fakeRegistry, fakeRmiServer, fakeSocketServer
        );
    }

    // ──────────────────────────────────────────────
    // TEST REQUEST HANDLING
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("handleClientRequest send ping and accept the request")
    void testHandleClientRequest_updatesPingAndAccepts() {
        final boolean[] acceptCalled = {false};
        ClientRequest fakeReq = new ClientRequest() {
            @Override
            public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
                acceptCalled[0] = true;
            }
        };

        serverController.handleClientRequest(fakeReq, client);

        assertTrue(acceptCalled[0], "Accept method not called");
        assertTrue(client.pingSent, "Ping not sent to client");
        assertTrue(client.getLastPing() > 0, "Last client ping not updated");
    }

    @Test
    @DisplayName("visit(ClientConnection) send a login request")
    void testVisitClientConnection_sendsLoginNeeded() throws ServerException {
        serverController.visit(new ClientConnection(), client);

        assertTrue(client.loginNeededSent);
    }

    // ──────────────────────────────────────────────
    // TEST LOGIN
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("Login with a new username succeed")
    void testVisitLoginRequest_success() throws ServerException {
        LoginRequest req = new LoginRequest("Player1");

        assertDoesNotThrow(() -> serverController.visit(req, client));
        assertTrue(client.getClientUsername().isPresent());
        assertEquals("Player1", client.getClientUsername().get());
    }

    @Test
    @DisplayName("Login with a already connected username failed")
    void testVisitLoginRequest_duplicateUsername_throwsException() throws ServerException {
        LoginRequest req1 = new LoginRequest("Player1");
        serverController.visit(req1, client);

        FakeVirtualClient client2 = new FakeVirtualClient();
        LoginRequest req2 = new LoginRequest("Player1");

        ServerException exception = assertThrows(ServerException.class,
                () -> serverController.visit(req2, client2));
    }

    // ──────────────────────────────────────────────
    // TEST GAME CREATION
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("Creation from a client not logged failed")
    void testVisitCreateGameRequest_notLogged_throwsException() {
        CreateGameRequest req = new CreateGameRequest(4);

        assertThrows(ServerException.class, () -> serverController.visit(req, client));
    }

    @Test
    @DisplayName("Creation with an invalid number of players failed")
    void testVisitCreateGameRequest_invalidPlayers_throwsException() {
        client.setClientUsername("Player1");

        assertThrows(ServerException.class, () -> serverController.visit(new CreateGameRequest(1), client));
        assertThrows(ServerException.class, () -> serverController.visit(new CreateGameRequest(6), client));
    }

    @Test
    @DisplayName("Creation with correct parameter succeed")
    void testVisitCreateGameRequest_success() throws Exception {
        client.setClientUsername("Player1");
        CreateGameRequest req = new CreateGameRequest(4);

        assertDoesNotThrow(() -> serverController.visit(req, client));

        assertEquals(1, fakeGameDAO.createdMatches.size(), "A game need to be created in the DB");

        assertTrue(client.getGameId().isPresent());
        assertEquals(1, client.getGameId().get());
    }

    // ──────────────────────────────────────────────
    // TEST IN-GAME REQUESTS (Start, Move, Enter)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("Entering in a not existing match failed")
    void testVisitEnterGameRequest_gameNotExists_throwsException() {
        client.setClientUsername("Player1");
        EnterGameRequest req = new EnterGameRequest(999);

        assertThrows(ServerException.class, () -> serverController.visit(req, client));
    }

    @Test
    @DisplayName("StartGame without an associated gameID failed")
    void testVisitStartGameRequest_noGameId_throwsException() {
        client.setClientUsername("Player1");
        StartGameRequest req = new StartGameRequest(); // Same for MoveRequest

        assertThrows(ServerException.class, () -> serverController.visit(req, client));
    }

    // ──────────────────────────────────────────────
    // TEST DISCONNECTION AND TIMEOUT
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("ClientDisconnected clean client's state")
    void testVisitClientDisconnected_cleansUp() throws ServerException {
        client.setClientUsername("Player1");
        ClientDisconnected req = new ClientDisconnected();

        serverController.visit(req, client);

        assertTrue(client.getGameId().isEmpty());
        assertFalse(client.isConnected());
    }

    @Test
    @DisplayName("Timeout checker disconnect inactive client")
    void testTimeoutChecker_disconnectsIdleClient() throws Exception {
        client.setClientUsername("Player1");
        LoginRequest req = new LoginRequest("Player1");
        serverController.visit(req, client);

        serverController.startTimeoutChecker(10, 50);

        Thread.sleep(100);
        serverController.stopTimeoutChecker();

        assertFalse(client.isConnected());
    }

    // ──────────────────────────────────────────────
    // TEST END GAME
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("notifyEndGame with match_results null remove from DB and disk")
    void testNotifyEndGame_withNullResults_cleansDatabase() {
        int targetGameId = 101;

        serverController.notifyEndGame(targetGameId, null);

        assertTrue(fakePersistence.removedGames.contains(targetGameId));
        assertTrue(fakeGameDAO.deletedMatches.contains(targetGameId));
    }
}
