package org.adsl.server.controller;

import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.requests.RequestVisitor;
import org.adsl.utils.builder.GameControllerBuilder;
import org.adsl.utils.fakes.FakeState;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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