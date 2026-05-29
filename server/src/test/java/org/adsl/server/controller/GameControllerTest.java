package org.adsl.server.controller;

import org.adsl.server.controller.states.LobbyState;
import org.adsl.server.model.Player;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.requests.RequestVisitor;
import org.adsl.utils.builder.GameControllerBuilder;
import org.adsl.utils.fakes.FakeGame;
import org.adsl.utils.fakes.FakeState;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameControllerTest {
    private GameControllerBuilder builder;
    private FakeVirtualClient client;
    private GameController controller;

    @BeforeEach
    void setUp() {
        builder = new GameControllerBuilder();
        client = new FakeVirtualClient();
        client.setClientUsername("Player1");
        controller = builder.build();
    }

    // ──────────────────────────────────────────────
    // TEST FSM
    // ──────────────────────────────────────────────

    @Test
    void testHandleClientRequest_routesToCurrentState() throws ServerException {
        FakeState initialState = new FakeState(controller);
        controller.setState(initialState);

        ClientRequest testRequest = new ClientRequest() {
            @Override
            public <T> void accept(RequestVisitor<T> visitor, T c) {
                if (visitor instanceof FakeState state) {
                    state.requestHandled = true;
                }
            }
        };

        controller.handleClientRequest(testRequest, client);

        assertTrue(initialState.requestHandled);
    }

    @Test
    void testStateEngine_singleTransition() throws ServerException {
        FakeState stateA = new FakeState(controller);
        FakeState stateB = new FakeState(controller);

        controller.setState(stateA);

        ClientRequest endTurnRequest = new ClientRequest() {
            @Override
            public <T> void accept(RequestVisitor<T> visitor, T c) {
                stateA.setNextState(stateB);
            }
        };

        controller.handleClientRequest(endTurnRequest, client);

        assertTrue(stateB.onEntryCalled);
    }

    @Test
    void testStateEngine_automaticChainTransitions() throws ServerException {
        FakeState stateA = new FakeState(controller);
        FakeState stateB = new FakeState(controller);
        FakeState stateC = new FakeState(controller);

        controller.setState(stateA);
        stateB.autoTransitionTarget = stateC;

        ClientRequest triggerRequest = new ClientRequest() {
            @Override
            public <T> void accept(RequestVisitor<T> visitor, T c) {
                stateA.setNextState(stateB);
            }
        };

        controller.handleClientRequest(triggerRequest, client);

        assertTrue(stateB.onEntryCalled);
        assertTrue(stateC.onEntryCalled);
    }

    // ──────────────────────────────────────────────
    // TEST getLobbyPlayers / getCapacity
    // ──────────────────────────────────────────────

    @Test
    void testGetLobbyPlayers_stateIsNull_returnsEmptyList() {
        GameController gc = new GameControllerBuilder().build();
        assertEquals(List.of(), gc.getLobbyPlayers());
    }

    @Test
    void testGetLobbyPlayers_withPlayers_returnsNames() {
        FakeGame game = new FakeGame(1, 3);
        game.getPlayers().add(new Player("Alice"));
        game.getPlayers().add(new Player("Bob"));
        GameController gc = new GameControllerBuilder().build();
        gc.setState(new LobbyState(game, gc, "Alice"));

        List<String> names = gc.getLobbyPlayers();

        assertEquals(2, names.size());
        assertTrue(names.contains("Alice"));
        assertTrue(names.contains("Bob"));
    }

    @Test
    void testGetCapacity_stateIsNull_returnsZero() {
        GameController gc = new GameControllerBuilder().build();
        assertEquals(0, gc.getCapacity());
    }

    @Test
    void testGetCapacity_withGame_returnsNumPlayer() {
        FakeGame game = new FakeGame(1, 4);
        GameController gc = new GameControllerBuilder().build();
        gc.setState(new LobbyState(game, gc, "Host"));

        assertEquals(4, gc.getCapacity());
    }

    @Test
    void testChangeState_infiniteLoop_throwsServerException() throws ServerException {
        FakeState stateA = new FakeState(controller);
        FakeState stateB = new FakeState(controller);

        stateA.autoTransitionTarget = stateB;
        stateB.autoTransitionTarget = stateA;

        controller.setState(stateA);

        ClientRequest trigger = new ClientRequest() {
            @Override
            public <T> void accept(RequestVisitor<T> visitor, T c) {
                stateA.setNextState(stateB);
            }
        };

        ServerException exception = assertThrows(ServerException.class,
                () -> controller.handleClientRequest(trigger, client));

        assertTrue(exception.getMessage().contains("loop detected"));
    }
}
