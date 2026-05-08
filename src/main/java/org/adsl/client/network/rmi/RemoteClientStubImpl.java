package org.adsl.client.network.rmi;

import org.adsl.client.AppCoordinator;
import org.adsl.shared.network.remote.RemoteClientStub;
import org.adsl.shared.network.responses.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class RemoteClientStubImpl extends UnicastRemoteObject implements RemoteClientStub {
    private final transient AppCoordinator coordinator;
    private final ExecutorService threadPool;

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

    public void sendResponse(ServerResponse response){
        threadPool.submit(() -> coordinator.handleServerResponse(response));
    }

    public void shutdown() {
        threadPool.shutdownNow();
    }
}
