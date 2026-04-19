package org.adsl.server.controller;

import org.adsl.utils.builder.ServerControllerBuilder;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.adsl.utils.fakes.FakeHome;
import org.adsl.utils.fakes.FakeVirtualClient;
import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.network.requests.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ServerControllerTest {
    private FakeVirtualClient client;
    private FakeGameDAO fakeGameDAO;
    private FakeGamePersistenceManager fakePersistence;
    private ServerControllerBuilder builder;
    private ServerController serverController;

    @BeforeEach
    void setUp() {
        client = new FakeVirtualClient();
        fakeGameDAO = new FakeGameDAO();
        fakePersistence = new FakeGamePersistenceManager();
        builder = new ServerControllerBuilder();
        serverController = builder.build();
    }

    // ──────────────────────────────────────────────
    // TEST REQUEST HANDLING
    // ──────────────────────────────────────────────

    @Test
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
    void testVisitClientConnection_sendsLoginNeeded() throws ServerException {
        ClientConnection req = new ClientConnection();

        serverController.visit(req, client);

        assertTrue(client.loginNeededSent);
    }

    // ──────────────────────────────────────────────
    // TEST LOGIN
    // ──────────────────────────────────────────────

    @Test
    void testVisitLoginRequest_success() throws ServerException {
        LoginRequest req = new LoginRequest("Player1");

        assertDoesNotThrow(() -> serverController.visit(req, client));

        assertTrue(client.getClientUsername().isPresent());
        assertEquals("Player1", client.getClientUsername().get());
    }

    @Test
    void testVisitLoginRequest_duplicateUsername_throwsException() throws ServerException {
        LoginRequest req1 = new LoginRequest("Player1");
        FakeVirtualClient client2 = new FakeVirtualClient();
        LoginRequest req2 = new LoginRequest("Player1");

        serverController.visit(req1, client);
        ServerException exception = assertThrows(ServerException.class, () -> serverController.visit(req2, client2));

        assertTrue(exception.getMessage().contains("already connected"));
    }

    @Test
    void testVisitLoginRequest_trueConcurrentAtomicInsertion() throws InterruptedException {
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        CountDownLatch readyLatch = new CountDownLatch(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        String sharedUsername = "ConcurrentPlayer";

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                FakeVirtualClient threadClient = new FakeVirtualClient();
                LoginRequest req = new LoginRequest(sharedUsername);

                try {
                    readyLatch.countDown();
                    startLatch.await();

                    serverController.visit(req, threadClient);
                    successCount.incrementAndGet();
                } catch (ServerException e) {
                    if (e.getMessage().contains("already connected")) {
                        exceptionCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertEquals(1, successCount.get(), "Exactly one thread must win the race and succeed");
        assertEquals(numThreads - 1, exceptionCount.get(), "All other simultaneous threads must be cleanly rejected");
    }

    @Test
    void testVisitLoginRequest_addsUserToHome() throws ServerException {
        FakeHome fakeHome = new FakeHome();
        ServerController serverController = builder.withHome(fakeHome).build();
        LoginRequest login = new LoginRequest("Player1");

        serverController.visit(login, client);

        assertTrue(fakeHome.updateCalled);
        assertTrue(fakeHome.addedObservers.contains(client));
    }

    // ──────────────────────────────────────────────
    // TEST GAME CREATION
    // ──────────────────────────────────────────────

    @Test
    void testVisitCreateGameRequest_notLogged_throwsException() {
        CreateGameRequest req = new CreateGameRequest(4);

        ServerException exception = assertThrows(ServerException.class, () -> serverController.visit(req, client));

        assertTrue(exception.getMessage().contains("not logged"));

    }

    @Test
    void testVisitCreateGameRequest_invalidPlayers_throwsException() {
        client.setClientUsername("Player1");

        ServerException exception1 = assertThrows(ServerException.class,
                () -> serverController.visit(new CreateGameRequest(1), client));
        ServerException exception2 = assertThrows(ServerException.class,
                () -> serverController.visit(new CreateGameRequest(6), client));

        assertTrue(exception1.getMessage().contains("invalid number"));
        assertTrue(exception2.getMessage().contains("invalid number"));
    }

    @Test
    void testVisitCreateGameRequest_success() throws Exception {
        ServerController serverController = builder.withGameDAO(fakeGameDAO).build();
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
    void testVisitEnterGameRequest_gameNotExists_throwsException() {
        client.setClientUsername("Player1");
        EnterGameRequest req = new EnterGameRequest(999);

        ServerException exception = assertThrows(ServerException.class, () -> serverController.visit(req, client));

        assertTrue(exception.getMessage().contains("not exists"));

    }

    @Test
    void testVisitStartGameRequest_noGameId_throwsException() {
        client.setClientUsername("Player1");
        StartGameRequest req = new StartGameRequest(); // Same for MoveRequest

        ServerException exception = assertThrows(ServerException.class, () -> serverController.visit(req, client));

        assertTrue(exception.getMessage().contains("no gameId"));

    }

    // ──────────────────────────────────────────────
    // TEST DISCONNECTION AND TIMEOUT
    // ──────────────────────────────────────────────

    @Test
    void testVisitClientDisconnected_cleansUp() throws ServerException {
        client.setClientUsername("Player1");
        ClientDisconnected req = new ClientDisconnected();

        serverController.visit(req, client);

        assertTrue(client.getGameId().isEmpty());
        assertFalse(client.isConnected());
    }

    @Test
    void testTimeoutChecker_disconnectsIdleClient() throws Exception {
        client.setClientUsername("Player1");
        LoginRequest req = new LoginRequest("Player1");

        serverController.visit(req, client);
        serverController.startTimeoutChecker(10, 50);
        Thread.sleep(250);
        serverController.stopTimeoutChecker();

        assertFalse(client.isConnected());
    }

    // ──────────────────────────────────────────────
    // TEST END GAME
    // ──────────────────────────────────────────────

    @Test
    void testNotifyEndGame_withNullResults_cleansDatabase() {
        ServerController serverController = builder.withPersistenceManager(fakePersistence)
                .withGameDAO(fakeGameDAO).build();
        int targetGameId = 101;

        serverController.notifyEndGame(targetGameId, null);

        assertTrue(fakePersistence.removedGames.contains(targetGameId));
        assertTrue(fakeGameDAO.deletedMatches.contains(targetGameId));
    }

    @Test
    void testNotifyEndGame_removeGameIdFromClientOnlyIfInGame() {
        int targetGameId = 101;

        client.setGameId(targetGameId);
        LoginRequest req = new LoginRequest("Player1");
        serverController.visit(req,client);

        FakeVirtualClient client2 = new FakeVirtualClient();
        client2.setGameId(10);
        LoginRequest req2 = new LoginRequest("Player2");
        serverController.visit(req2,client2);

        serverController.notifyEndGame(targetGameId, null);

        assertTrue(client.getGameId().isEmpty());
        assertTrue(client2.getGameId().isPresent());
        assertEquals(10, client2.getGameId().get());
    }
}
