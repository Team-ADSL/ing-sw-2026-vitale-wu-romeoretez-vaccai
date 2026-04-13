package org.example.server.controller.states;

import org.example.server.config.BoardConfigLoader;
import org.example.server.config.JsonBoardConfigLoader;
import org.example.server.controller.GameController;
import org.example.server.controller.ServerController;
import org.example.server.model.Game;
import org.example.server.network.VirtualClient;
import org.example.server.persistence.GameDAO;
import org.example.server.persistence.GamePersistenceManager;
import org.example.server.exceptions.GameException;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.example.shared.network.requests.ClientDisconnected;
import org.example.shared.network.requests.EnterGameRequest;
import org.example.shared.network.requests.StartGameRequest;
import org.example.shared.network.responses.ServerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LobbyStateTest {
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

    // Concrete VirtualClient for testing
    static class TestVirtualClient extends VirtualClient {
        TestVirtualClient(ServerController sc, String username) {
            super(sc);
            setClientUsername(username);
        }
        @Override
        public void sendResponse(ServerResponse response) {}
    }

    private Game game;
    private GameController controller;
    private LobbyState state;
    private ServerController serverController;

    @BeforeEach
    void setUp() {
        BoardConfigLoader loader = new JsonBoardConfigLoader();
        serverController = new ServerController(NO_OP_DAO, loader, NO_OP_PERSISTENCE);
        game = new Game(1, 2);
        controller = new GameController(loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        state = new LobbyState(game, controller);
    }

    private TestVirtualClient client(String username) {
        return new TestVirtualClient(serverController, username);
    }

    // ──────────────────────────────────────────────
    // visit(EnterGameRequest, ...)
    // ──────────────────────────────────────────────

    @Test
    void enterGame_addsPlayerToGame() throws GameException {
        TestVirtualClient c = client("alice");
        state.visit(new EnterGameRequest(1), c);
        assertEquals(1, game.getPlayers().size());
        assertTrue(game.getPlayers().stream().anyMatch(p -> p.getName().equals("alice")));
    }

    @Test
    void enterGame_setsGameControllerOnClient() throws GameException {
        TestVirtualClient c = client("alice");
        state.visit(new EnterGameRequest(1), c);
        assertTrue(c.getGameId().isPresent());
        assertSame(game.getGameId(), c.getGameId().get());
    }

    @Test
    void enterGame_throwsWhenLobbyFull() throws GameException {
        // Fill to 3 players
        for (int i = 0; i < 2; i++) {
            TestVirtualClient c = client("player" + i);
            state.visit(new EnterGameRequest(1), c);
        }
        TestVirtualClient extra = client("extra");
        assertThrows(GameException.class, () -> state.visit(new EnterGameRequest(1), extra));
    }

    // ──────────────────────────────────────────────
    // visit(ClientDisconnected, ...)
    // ──────────────────────────────────────────────

    @Test
    void clientDisconnected_removesPlayer() throws GameException {
        TestVirtualClient c = client("bob");
        state.visit(new EnterGameRequest(1), c);
        assertEquals(1, game.getPlayers().size());

        state.visit(new ClientDisconnected(), c);
        assertEquals(0, game.getPlayers().size());
    }

    @Test
    void clientDisconnected_clearsGameControllerOnClient() throws GameException {
        TestVirtualClient c = client("bob");
        state.visit(new EnterGameRequest(1), c);
        state.visit(new ClientDisconnected(), c);
        assertFalse(c.getGameId().isPresent());
    }

    @Test
    void clientDisconnected_throwsWhenPlayerNotInGame() {
        TestVirtualClient c = client("notInGame");
        // The player was never added — disconnect should throw
        assertThrows(GameException.class, () -> state.visit(new ClientDisconnected(), c));
    }

    // ──────────────────────────────────────────────
    // visit(StartGameRequest, ...)
    // ──────────────────────────────────────────────

    @Test
    void startGame_setsReadyWhenTwoPlayers() throws GameException {
        state.visit(new EnterGameRequest(1), client("p1"));
        state.visit(new EnterGameRequest(1), client("p2"));

        state.visit(new StartGameRequest(), client("p1"));

        // nextState should now return InitGameState (not this)
        assertInstanceOf(InitGameState.class, state.calcNextState());
    }

    @Test
    void startGame_throwsWhenLessThanTwoPlayers() throws GameException {
        state.visit(new EnterGameRequest(1), client("p1"));
        assertThrows(GameException.class, () -> state.visit(new StartGameRequest(), client("p1")));
    }

    // ──────────────────────────────────────────────
    // nextState
    // ──────────────────────────────────────────────

    @Test
    void nextState_returnsSelfWhenNotReady() {
        assertSame(state, state.calcNextState());
    }

    @Test
    void nextState_returnsInitGameStateWhenReady() throws GameException {
        state.visit(new EnterGameRequest(1), client("p1"));
        state.visit(new EnterGameRequest(1), client("p2"));
        state.visit(new StartGameRequest(), client("p1"));

        assertInstanceOf(InitGameState.class, state.calcNextState());
    }
}
