package org.example.server.controller;

import org.example.server.config.BoardConfigLoader;
import org.example.server.config.JsonBoardConfigLoader;
import org.example.server.controller.states.LobbyState;
import org.example.server.model.EndGameObserver;
import org.example.server.model.Game;
import org.example.server.persistence.GameDAO;
import org.example.server.persistence.GamePersistenceManager;
import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameControllerTest {

    private static final EndGameObserver NO_OP_END = (id, r) -> {};
    private static final GamePersistenceManager NO_OP_PERSISTENCE = new GamePersistenceManager() {
        @Override public List<Game> recoverGames() { return List.of(); }
        @Override public void removeGame(int id) {}
        @Override public void updateHome(List<Integer> g) {}
        @Override public void updateLobby(List<String> p) {}
        @Override public void updateGame(GameDTO g) {}
    };
    private static final GameDAO NO_OP_DAO = new GameDAO() {
        @Override public int createMatch() { return 0; }
        @Override public void deleteMatch(int id) {}
        @Override public void saveMatch(int id, int c, List<String> n, List<Integer> s) {}
        @Override public List<MatchResult> getLeaderboard(int c) { return List.of(); }
    };

    private BoardConfigLoader loader;
    private Game game;
    private GameController controller;

    @BeforeEach
    void setUp() {
        loader = new JsonBoardConfigLoader();
        game = new Game(1, NO_OP_END);
        controller = new GameController(game, loader, NO_OP_PERSISTENCE, NO_OP_DAO);
    }

    // ──────────────────────────────────────────────
    // Constructor / getters
    // ──────────────────────────────────────────────

    @Test
    void getBoardConfigLoader_returnsPassedLoader() {
        assertSame(loader, controller.getBoardConfigLoader());
    }

    @Test
    void getGameDAO_returnsPassedDAO() {
        assertSame(NO_OP_DAO, controller.getGameDAO());
    }

    @Test
    void getPersistenceManager_returnsPassedManager() {
        assertSame(NO_OP_PERSISTENCE, controller.getPersistenceManager());
    }

    @Test
    void getState_isLobbyStateInitially() {
        assertInstanceOf(LobbyState.class, controller.getState());
    }

    // ──────────────────────────────────────────────
    // Different instances are independent
    // ──────────────────────────────────────────────

    @Test
    void twoControllers_haveDifferentStates() {
        Game game2 = new Game(2, NO_OP_END);
        GameController controller2 = new GameController(game2, loader, NO_OP_PERSISTENCE, NO_OP_DAO);
        assertNotSame(controller.getState(), controller2.getState());
    }

    @Test
    void getBoardConfigLoader_differentLoadersForDifferentControllers() {
        BoardConfigLoader loader2 = new JsonBoardConfigLoader();
        GameController controller2 = new GameController(new Game(2, NO_OP_END), loader2, NO_OP_PERSISTENCE, NO_OP_DAO);
        assertNotSame(loader, controller2.getBoardConfigLoader());
    }
}
