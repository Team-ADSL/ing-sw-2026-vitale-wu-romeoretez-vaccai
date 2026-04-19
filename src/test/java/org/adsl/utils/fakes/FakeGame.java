package org.adsl.utils.fakes;

import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;

import java.util.*;

public class FakeGame extends Game {
    public final Set<Player> players = new HashSet<>();
    public Player currentPlayer;
    public boolean clientRemoved = false;

    public FakeGame() {
        super(1, 2);
    }

    @Override
    public Set<Player> getPlayers() {
        return players;
    }

    @Override
    public Optional<Player> getCurrentPlayer() {
        return Optional.ofNullable(currentPlayer);
    }

    @Override
    public void removeVirtualClient(VirtualClient virtualClient) {
        this.clientRemoved = true;
    }
}
