package org.adsl.server.network;

import org.adsl.server.controller.ServerController;
import org.adsl.server.network.socket.SocketClientHandler;
import org.adsl.shared.network.requests.ClientConnection;
import org.adsl.shared.network.requests.ClientDisconnected;
import org.adsl.shared.network.requests.ClientPing;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.utils.builder.ServerControllerBuilder;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SocketClientHandlerTest {

    static class FakeSocket extends Socket {
        private final ByteArrayInputStream inputStream;
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private volatile boolean closed = false;

        FakeSocket(byte[] inputData) {
            this.inputStream = new ByteArrayInputStream(inputData);
        }

        @Override public InputStream getInputStream() { return inputStream; }
        @Override public OutputStream getOutputStream() { return outputStream; }
        @Override public boolean isClosed() { return closed; }
        @Override public synchronized void close() { closed = true; }
    }

    static class RecordingServerController extends ServerController {
        final List<ClientRequest> handled = new ArrayList<>();

        RecordingServerController() {
            super(new org.adsl.utils.fakes.FakeHome(),
                    new org.adsl.utils.fakes.FakeGameDAO(),
                    new org.adsl.utils.TestDummies.DummyBoardConfigLoader(),
                    new org.adsl.utils.fakes.FakeGamePersistenceManager(),
                    new org.adsl.utils.TestDummies.DummyRegistry(),
                    new org.adsl.utils.TestDummies.DummyRemoteServerService(),
                    new org.adsl.utils.TestDummies.DummySocketServer());
        }

        @Override
        public void handleClientRequest(ClientRequest req, VirtualClient virtualClient) {
            handled.add(req);
        }
    }

    private static byte[] serializeRequests(ClientRequest... requests) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            for (ClientRequest req : requests) {
                oos.writeObject(req);
            }
            oos.flush();
        }
        return baos.toByteArray();
    }

    // ──────────────────────────────────────────────
    // RUN — connection and request routing
    // ──────────────────────────────────────────────

    @Test
    void testRun_onEntry_routesClientConnectionToServerController() throws IOException {
        byte[] emptyStream = serializeRequests();
        FakeSocket socket = new FakeSocket(emptyStream);
        RecordingServerController controller = new RecordingServerController();

        new SocketClientHandler(socket, controller).run();

        assertFalse(controller.handled.isEmpty(), "handleConnection must route ClientConnection to ServerController");
        assertInstanceOf(ClientConnection.class, controller.handled.getFirst());
    }

    @Test
    void testRun_serializedRequest_isRoutedToServerController() throws IOException {
        byte[] stream = serializeRequests(new ClientPing());
        FakeSocket socket = new FakeSocket(stream);
        RecordingServerController controller = new RecordingServerController();

        new SocketClientHandler(socket, controller).run();

        boolean pingHandled = controller.handled.stream()
                .anyMatch(r -> r instanceof ClientPing);
        assertTrue(pingHandled, "Deserialized ClientPing must be routed to ServerController");
    }

    @Test
    void testRun_streamExhausted_triggersDisconnection() throws IOException {
        byte[] emptyStream = serializeRequests();
        FakeSocket socket = new FakeSocket(emptyStream);
        RecordingServerController controller = new RecordingServerController();

        new SocketClientHandler(socket, controller).run();

        boolean disconnectHandled = controller.handled.stream()
                .anyMatch(r -> r instanceof ClientDisconnected);
        assertTrue(disconnectHandled, "When stream is exhausted, ClientDisconnected must be routed");
    }

    @Test
    void testRun_corruptInputStream_triggersDisconnection() {
        byte[] garbage = {0x00, 0x01, 0x02, 0x03};
        FakeSocket socket = new FakeSocket(garbage);
        RecordingServerController controller = new RecordingServerController();

        new SocketClientHandler(socket, controller).run();

        boolean disconnectHandled = controller.handled.stream()
                .anyMatch(r -> r instanceof ClientDisconnected);
        assertTrue(disconnectHandled, "IOException from corrupt stream must trigger ClientDisconnected");
    }

    // ──────────────────────────────────────────────
    // closeConnection
    // ──────────────────────────────────────────────

    @Test
    void testCloseConnection_closesUnderlyingSocket() throws IOException {
        byte[] emptyStream = serializeRequests();
        FakeSocket socket = new FakeSocket(emptyStream);
        RecordingServerController controller = new RecordingServerController();
        SocketClientHandler handler = new SocketClientHandler(socket, controller);

        handler.run();
        handler.closeConnection();

        assertTrue(socket.isClosed(), "closeConnection must close the underlying socket");
    }
}
