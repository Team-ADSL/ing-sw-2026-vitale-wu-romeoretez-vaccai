package org.adsl.server.network;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.ServerController;
import org.adsl.server.model.Game;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.network.requests.ClientConnection;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.responses.ServerResponse;
import org.adsl.utils.fakes.FakeHome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VirtualClientTest {

    private static final GamePersistenceManager NO_OP_PERSISTENCE = new GamePersistenceManager() {
        @Override public List<Game> recoverGames() { return List.of(); }
        @Override public void removeGame(int id) {}
        @Override public void updateLobby(int gameId, List<String> p, int numPlayers) {}
        @Override
        public void updateTotemAvailable(List<Totem> totemsAvailable, String message) {}
        @Override public void updateGame(Game g) {}
    };
    private static final GameDAO NO_OP_DAO = new GameDAO() {
        @Override public int createMatch() { return 0; }
        @Override public void deleteMatch(int id) {}
        @Override public void saveMatch(int id, int c, List<String> n, List<Integer> s) {}
        @Override public List<DBRecord> getLeaderboard(int c) { return List.of(); }
        @Override public void setInitialCounter(int i) {}
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
        @Override
        public void closeConnection() {}
    }

    /** ServerController that records which requests it handled. */
    static class RecordingServerController extends ServerController {
        final List<ClientRequest> handled = new ArrayList<>();

        RecordingServerController(GameDAO dao, BoardConfigLoader loader, GamePersistenceManager pm) {
            super(new FakeHome(), dao, loader, pm, null, null, null);
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

    // ──────────────────────────────────────────────
    // isConnected / setConnected
    // ──────────────────────────────────────────────

    @Test
    void isConnected_defaultTrue() {
        assertTrue(client.isConnected());
    }

    @Test
    void setConnected_false_returnsDisconnected() {
        client.setConnected(false);
        assertFalse(client.isConnected());
    }

    @Test
    void setConnected_toggle_restoresConnected() {
        client.setConnected(false);
        client.setConnected(true);
        assertTrue(client.isConnected());
    }

    // ──────────────────────────────────────────────
    // gameId
    // ──────────────────────────────────────────────

    @Test
    void getGameId_nullByDefault() {
        assertTrue(client.getGameId().isEmpty());
    }

    @Test
    void setGameId_thenGetGameId_returnsValue() {
        client.setGameId(7);
        assertTrue(client.getGameId().isPresent());
        assertEquals(7, client.getGameId().get());
    }

    @Test
    void setGameId_null_clearsGameId() {
        client.setGameId(1);
        client.setGameId(null);
        assertTrue(client.getGameId().isEmpty());
    }
}
