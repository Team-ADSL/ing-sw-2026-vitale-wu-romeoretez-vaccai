package org.adsl.client.network.fake;

import org.adsl.client.AppCoordinator;
import org.adsl.client.network.ServerConnection;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.BoardDTO;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.OfferTileDTO;
import org.adsl.shared.model.OrderTileDTO;
import org.adsl.shared.model.PlayerDTO;
import org.adsl.shared.network.requests.ClientConnection;
import org.adsl.shared.network.requests.ClientDisconnected;
import org.adsl.shared.network.requests.ClientPing;
import org.adsl.shared.network.requests.ClientRequest;
import org.adsl.shared.network.requests.CreateGameRequest;
import org.adsl.shared.network.requests.EnterGameRequest;
import org.adsl.shared.network.requests.LoginRequest;
import org.adsl.shared.network.requests.MoveRequest;
import org.adsl.shared.network.requests.StartGameRequest;
import org.adsl.shared.network.responses.GameUpdate;
import org.adsl.shared.network.responses.HomeUpdate;
import org.adsl.shared.network.responses.LobbyUpdate;
import org.adsl.shared.network.responses.LoginNeeded;
import org.adsl.shared.network.responses.ServerPing;
import org.adsl.shared.network.responses.ServerResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * In-process fake server used for TUI testing.
 *
 * Accepts any request, always replies with an "empty" / unchanged state:
 *   • ClientConnection   → LoginNeeded
 *   • LoginRequest       → HomeUpdate (no active games)
 *   • CreateGameRequest  → LobbyUpdate with the caller as the only player
 *   • EnterGameRequest   → LobbyUpdate with the caller as the only player
 *   • StartGameRequest   → GameUpdate with a minimal playable snapshot
 *   • MoveRequest        → same GameUpdate snapshot resent (new reference, same content)
 *   • ClientPing         → ServerPing
 *   • ClientDisconnected → no-op
 *
 * A move therefore never produces a visual change — the purpose is to let the
 * user walk through the full screen flow without having to run a real server.
 */
public class FakeServerConnection implements ServerConnection {
    private AppCoordinator appCoordinator;

    private String username = "player";
    private int numPlayers = 2;
    private GameDTO snapshot;

    @Override
    public void connect(String ip, int port) { /* no-op */ }

    @Override
    public void disconnect() { /* no-op */ }

    @Override
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }

    @Override
    public void sendRequest(ClientRequest request) {
        if (appCoordinator == null || request == null) return;

        ServerResponse response = switch (request) {
            case ClientConnection   r -> new LoginNeeded();
            case LoginRequest       r -> { username = r.getUsername(); yield new HomeUpdate(Collections.emptyList()); }
            case CreateGameRequest  r -> { numPlayers = r.getNumPlayer(); yield new LobbyUpdate(List.of(username), r.getNumPlayer()); }
            case EnterGameRequest   r -> new LobbyUpdate(List.of(username), 5);
            case StartGameRequest   r -> { snapshot = buildSnapshot(); yield new GameUpdate(snapshot); }
            case MoveRequest        r -> new GameUpdate(resendSnapshot());
            case ClientPing         r -> new ServerPing();
            case ClientDisconnected r -> null;
            default                   -> null;
        };

        if (response != null) {
            appCoordinator.handleServerResponse(response);
        }
    }

    // ── Snapshot builders ─────────────────────────────────────────────────────

    /** Builds a minimal, playable game state: the user gets RED and it's their turn to place a totem. */
    private GameDTO buildSnapshot() {
        Map<CardType, Set<org.adsl.shared.model.CardDTO>> emptyCards = new EnumMap<>(CardType.class);
        for (CardType t : CardType.values()) emptyCards.put(t, new HashSet<>());

        PlayerDTO me = new PlayerDTO(username, 0, 0, Totem.RED, emptyCards);

        Set<PlayerDTO> players = new LinkedHashSet<>();
        players.add(me);

        ArrayList<OfferTileDTO> offerTrack = new ArrayList<>();
        offerTrack.add(new OfferTileDTO("offer_tile_b", null, Collections.emptyMap(), false));
        offerTrack.add(new OfferTileDTO("offer_tile_c", null, Collections.emptyMap(), false));
        offerTrack.add(new OfferTileDTO("offer_tile_e", null, Collections.emptyMap(), false));
        offerTrack.add(new OfferTileDTO("offer_tile_f", null, Collections.emptyMap(), false));

        ArrayList<Totem> turnOrder = new ArrayList<>();
        turnOrder.add(Totem.RED);
        OrderTileDTO orderTile = new OrderTileDTO("order_tile_01", turnOrder);

        ArrayList<Boolean> buildings = new ArrayList<>();
        buildings.add(true);
        buildings.add(true);
        buildings.add(true);

        BoardDTO board = new BoardDTO(
                new ArrayList<>(),   // lowRow — empty
                new ArrayList<>(),   // topRow — empty
                offerTrack,
                orderTile,
                buildings,
                false
        );

        return new GameDTO(1, numPlayers, 1, 1, players, board, Phase.TOTEM_PLACEMENT, Totem.RED);
    }

    /**
     * Returns a new GameDTO with identical content to the last snapshot. The
     * reference is fresh so the TUI's "wait for state change" check wakes up,
     * but since the data is the same the screen does not change.
     */
    private GameDTO resendSnapshot() {
        if (snapshot == null) snapshot = buildSnapshot();
        return new GameDTO(
                snapshot.id(),
                snapshot.numPlayer(),
                snapshot.round(),
                snapshot.era(),
                snapshot.players(),
                snapshot.board(),
                snapshot.phase(),
                snapshot.currentPlayerTotem()
        );
    }
}
