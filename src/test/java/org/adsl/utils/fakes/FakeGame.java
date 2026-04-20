package org.adsl.utils.fakes;

import org.adsl.server.model.Game;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.model.MatchResult;

import java.util.ArrayList;
import java.util.List;

public class FakeGame extends Game {
    public boolean updateLobbySent = false;
    public boolean updateGameSent = false;
    public boolean endGameResultsSent = false;
    public List<MatchResult> capturedResults = null;
    public final List<VirtualClient> addedClients = new ArrayList<>();
    public final List<VirtualClient> removedClients = new ArrayList<>();

    public FakeGame(int gameId, int numPlayer) {
        super(gameId, numPlayer);
    }

    public FakeGame() {
        super(1, 5);
    }

    @Override
    public void addVirtualClient(VirtualClient virtualClient) {
        this.addedClients.add(virtualClient);
    }

    @Override
    public void removeVirtualClient(VirtualClient virtualClient) {
        this.removedClients.add(virtualClient);
    }

    @Override
    public void sendUpdateLobby() {
        this.updateLobbySent = true;
    }

    @Override
    public void sendUpdateGame() {
        this.updateGameSent = true;
    }

    @Override
    public void sendEndGameResults(List<MatchResult> results) {
        this.endGameResultsSent = true;
        this.capturedResults = results;
    }
}
