package org.example.server.network;

import org.example.server.config.BoardConfigLoader;
import org.example.server.config.JsonBoardConfigLoader;
import org.example.server.controller.GameController;
import org.example.server.controller.ServerController;
import org.example.server.model.Game;
import org.example.server.persistence.GameDAO;
import org.example.server.persistence.GamePersistenceManager;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.example.shared.network.requests.ClientConnection;
import org.example.shared.network.requests.ClientDisconnected;
import org.example.shared.network.requests.ClientRequest;
import org.example.shared.network.responses.ServerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualClientTest {

    private static final GamePersistenceManager NO_OP_PERSISTENCE = new GamePersistenceManager() {
        @Override public List<Game> recoverGames() { return List.of(); }
        @Override public void removeGame(int id) {}
        @Override public void updateLobby(List<String> p) {}
        @Override public void updateGame(GameDTO g) {}
    };
    private static final GameDAO NO_OP_DAO = new GameDAO() {
        @Override public int createMatch() { return 0; }
        @Override public void deleteMatch(int id) {}
        @Override public void saveMatch(int id, int c, List<String> n, List<Integer> s) {}
        @Override public List<MatchResult> getLeaderboard(int c) { return List.of(); }
    };

    /** Concrete VirtualClient for testing — records last response sent. */
    static class TestVirtualClient extends VirtualClient {
        final List<ServerResponse> sent = new ArrayList<>();
        final List<ClientRequest> processedRequests = new ArrayList<>();

        TestVirtualClient(ServerController sc) {
            super(sc);
        }

        @Override
        public void sendResponse(ServerResponse response) {
            sent.add(response);
        }
    }

    /** ServerController that records which requests it handled. */
    static class RecordingServerController extends ServerController {
        final List<ClientRequest> handled = new ArrayList<>();

        RecordingServerController(GameDAO dao, BoardConfigLoader loader, GamePersistenceManager pm) {
            super(dao, loader, pm);
        }

        @Override
        public void handleClientRequest(ClientRequest req, VirtualClient virtualClient) {
            handled.add(req);
        }
    }

    private RecordingServerController serverController;
    private TestVirtualClient client;

    @BeforeEach
    void setUp() {
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        serverController = new RecordingServerController(NO_OP_DAO, loader, NO_OP_PERSISTENCE);
        client = new TestVirtualClient(serverController);
    }

    // ──────────────────────────────────────────────
    // processRequest — routing
    // ──────────────────────────────────────────────

    @Test
    void processRequest_routesToServerControllerWhenNoGameController() {
        ClientRequest req = new ClientConnection();
        client.processRequest(req);
        assertEquals(1, serverController.handled.size());
        assertSame(req, serverController.handled.getFirst());
    }

    // ──────────────────────────────────────────────
    // username
    // ──────────────────────────────────────────────

    @Test
    void getClientUsername_nullByDefault() {
        assertTrue(client.getClientUsername().isEmpty());
    }

    @Test
    void setClientUsername_thenGetClientUsername_returnsIt() {
        client.setClientUsername("alice");
        assertTrue(client.getClientUsername().isPresent());
        assertEquals("alice", client.getClientUsername().get());
    }

    // ──────────────────────────────────────────────
    // sendErrorMessage — delegates to sendResponse
    // ──────────────────────────────────────────────

    @Test
    void sendErrorMessage_addsSentResponse() {
        client.sendErrorMessage("boom");
        assertEquals(1, client.sent.size());
    }
}
