package org.adsl.utils;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.GameSettings;
import org.adsl.server.model.board.OfferTrack;
import org.adsl.server.model.board.OrderTile;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.buildings.Building;
import org.adsl.server.network.socket.SocketServer;
import org.adsl.shared.network.remote.RemoteClientStub;
import org.adsl.shared.network.remote.RemoteServerService;
import org.adsl.shared.network.requests.ClientRequest;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Set;

public class TestDummies {
    public static class DummyRemoteServerService implements RemoteServerService {
        @Override
        public void connect(RemoteClientStub clientStub) throws RemoteException {}
        @Override
        public void sendRequest(ClientRequest request, RemoteClientStub clientStub) throws RemoteException {}
        @Override
        public void disconnect(RemoteClientStub clientStub) {}
    }

    public static class DummyBoardConfigLoader implements BoardConfigLoader {
        @Override
        public ArrayList<Set<Card>> getCards(int numPlayers) {return null;}
        @Override
        public OfferTrack getOfferTrack(int numPlayers) {return null;}
        @Override
        public OrderTile getOrderTile(int numPlayers) {return null;}
        @Override
        public Set<Building> getBuildings() {return Set.of();}
        @Override
        public GameSettings getSettings(int numPlayers) {return null;}
    }

    public static class DummySocketServer extends SocketServer {
        public boolean shutdownCalled = false;
        public DummySocketServer() { super(null, 0); }
        @Override public void shutdown() { shutdownCalled = true; }
    }
}
