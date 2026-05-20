package org.adsl.utils.fakes;

import org.adsl.server.model.Game;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;

import java.util.ArrayList;
import java.util.List;

public class FakeGame extends Game {
    public boolean updateLobbySent = false;
    public boolean updateGameSent = false;
    public boolean endGameResultsSent = false;
    public boolean sendTotemAvailableSent = false;
    public List<DBRecord> capturedRecords = null;
    public List<MatchResult> capturedResults = null;
    public List<Totem> lastTotemAvailable = null;
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
    public void sendTotemAvailable(List<Totem> totemAvailable, String message) {
        this.sendTotemAvailableSent = true;
        this.lastTotemAvailable = new ArrayList<>(totemAvailable);
    }

    @Override
    public void sendUpdateLobby() {
        this.updateLobbySent = true;
    }

    @Override
    public void sendUpdateLobby(String message) {
        this.updateLobbySent = true;
    }

    @Override
    public void sendUpdateGame() {
        this.updateGameSent = true;
    }

    @Override
    public void sendUpdateGame(String message) {
        this.updateGameSent = true;
    }

    @Override
    public void sendEndGameResults(List<MatchResult> results, List<DBRecord> records) {
        this.endGameResultsSent = true;
        this.capturedRecords = records;
        this.capturedResults = results;
    }

    @Override
    public void sendEndGameResults(List<MatchResult> results, List<DBRecord> records, String message) {
        this.endGameResultsSent = true;
        this.capturedRecords = records;
        this.capturedResults = results;
    }
}
