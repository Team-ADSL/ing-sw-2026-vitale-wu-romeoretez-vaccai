package org.adsl.client.network.rmi;

import org.adsl.client.AppCoordinator;
import org.adsl.shared.network.remote.RemoteClientStub;
import org.adsl.shared.network.responses.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Client-side RMI callback object exported as a {@code UnicastRemoteObject}.
 * The server holds a reference to this stub and calls {@link #sendResponse}
 * to push responses asynchronously. Delivery is dispatched on a dedicated
 * single-threaded executor (daemon thread) to keep the RMI thread free and
 * to serialise responses for the {@code AppCoordinator}.
 */
public class RemoteClientStubImpl extends UnicastRemoteObject implements RemoteClientStub {
    private final transient AppCoordinator coordinator;
    private final ExecutorService threadPool;

    /**
     * Exports this object as a remote callback stub and prepares its
     * response-dispatch executor.
     *
     * @param coordinator the coordinator that will handle responses pushed
     *                     by the server
     * @throws RemoteException if exporting the remote object fails
     */
    public RemoteClientStubImpl(AppCoordinator coordinator) throws RemoteException {
        super();
        this.coordinator = coordinator;
        // Daemon threads so the executor never keeps the JVM alive after a
        // disconnect; explicit shutdown() also kills any in-flight task.
        this.threadPool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "rmi-client-stub");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Called remotely by the server to deliver a response. The response is
     * handed off to the dispatch executor so this RMI call returns
     * immediately and responses are processed in order on a single thread.
     *
     * @param response the server response to deliver to the coordinator
     */
    public void sendResponse(ServerResponse response){
        threadPool.submit(() -> coordinator.handleServerResponse(response));
    }

    /**
     * Stops the response-dispatch executor, discarding any pending or
     * in-flight dispatch tasks.
     */
    public void shutdown() {
        threadPool.shutdownNow();
    }
}
