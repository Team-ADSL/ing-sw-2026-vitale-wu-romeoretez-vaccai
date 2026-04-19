package org.adsl.utils.builder;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.controller.ServerController;
import org.adsl.server.model.Home;
import org.adsl.server.network.socket.SocketServer;
import org.adsl.server.persistence.GameDAO;
import org.adsl.server.persistence.GamePersistenceManager;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.utils.TestDummies;
import org.adsl.utils.fakes.FakeGameDAO;
import org.adsl.utils.fakes.FakeGamePersistenceManager;
import org.adsl.utils.fakes.FakeHome;
import org.adsl.utils.fakes.FakeRegistry;

import java.rmi.registry.Registry;

public class ServerControllerBuilder {
    public Home home = new FakeHome();
    public GameDAO gameDAO = new FakeGameDAO();
    public GamePersistenceManager persistenceManager = new FakeGamePersistenceManager();
    public Registry registry = new FakeRegistry();
    public BoardConfigLoader boardConfigLoader = new TestDummies.DummyBoardConfigLoader();
    public RemoteServerService rmiServer = new TestDummies.DummyRemoteServerService();
    public SocketServer socketServer = new TestDummies.DummySocketServer();

    public ServerController build(){
        return new ServerController(home, gameDAO, boardConfigLoader, persistenceManager,
                registry, rmiServer, socketServer);
    }

    public ServerControllerBuilder withGameDAO(GameDAO gameDAO){
        this.gameDAO = gameDAO;
        return this;
    }

    public ServerControllerBuilder withPersistenceManager(GamePersistenceManager persistenceManager){
        this.persistenceManager = persistenceManager;
        return this;
    }

    public ServerControllerBuilder withHome(Home home){
        this.home = home;
        return this;
    }
}
