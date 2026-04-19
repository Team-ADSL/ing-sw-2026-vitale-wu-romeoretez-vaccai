package org.adsl.client.local;

import org.adsl.server.config.BoardConfigLoader;
import org.adsl.server.config.JsonBoardConfigLoader;
import org.adsl.server.controller.ServerController;
import org.adsl.server.model.EndGameObserver;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.PlayerDTO;
import org.adsl.shared.network.requests.*;
import org.adsl.shared.network.responses.*;
import org.adsl.shared.utils.Move;

import java.util.*;
import java.util.function.Consumer;

/**
 * Manages a local multi-player game without a network layer.
 *
 * All N players share the same terminal. The TUI routes moves for the current
 * player; this coordinator dispatches them to the correct LocalVirtualClient.
 *
 * Only the first player's client forwards server responses to the TUI callback;
 * other clients silently drop responses to avoid duplicate updates.
 */
public class LocalGameCoordinator {
    private final ServerController serverController;
    private final NoOpGameDAO gameDAO;
    private final List<String> playerNames;
    private final Map<String, LocalVirtualClient> clientsByName;
    private final Map<Totem, String> totemToName;
    private volatile GameDTO lastGameState;
    private final Consumer<ServerResponse> tuiCallback;

    public LocalGameCoordinator(List<String> playerNames, Consumer<ServerResponse> tuiCallback) {
        this.playerNames = new ArrayList<>(playerNames);
        this.tuiCallback = tuiCallback;
        this.clientsByName = new LinkedHashMap<>();
        this.totemToName = new EnumMap<>(Totem.class);
        this.gameDAO = new NoOpGameDAO();

        BoardConfigLoader boardConfigLoader = new JsonBoardConfigLoader();
        NoOpGamePersistenceManager persistence = new NoOpGamePersistenceManager();

        // null for registry/rmiServer/socketServer: local mode does not use RMI or sockets
        this.serverController = new ServerController(
                gameDAO, boardConfigLoader, persistence, null, null, null);

        // Create one LocalVirtualClient per player.
        // Only player 1's client forwards responses to the TUI to avoid N identical updates.
        for (int i = 0; i < playerNames.size(); i++) {
            String name = playerNames.get(i);
            Consumer<ServerResponse> handler = (i == 0)
                    ? this::handlePrimaryResponse
                    : resp -> {};
            LocalVirtualClient client = new LocalVirtualClient(serverController, handler);
            clientsByName.put(name, client);
        }
    }

    /**
     * Logs in all players, creates the game, and starts it.
     * All responses arrive synchronously within this call.
     */
    public void initializeGame() {
        // Login all players
        for (String name : playerNames) {
            LocalVirtualClient client = clientsByName.get(name);
            client.handleConnection();
            client.processRequest(new LoginRequest(name));
        }

        // Player 1 creates and immediately enters the game (server handles EnterGame internally)
        clientsByName.get(playerNames.get(0)).processRequest(new CreateGameRequest(playerNames.size()));
        int gameId = gameDAO.getLastCreatedId();

        // Players 2-N join the lobby
        for (int i = 1; i < playerNames.size(); i++) {
            clientsByName.get(playerNames.get(i)).processRequest(new EnterGameRequest(gameId));
        }

        // Player 1 starts the game
        clientsByName.get(playerNames.get(0)).processRequest(new StartGameRequest());
    }

    /**
     * Submits moves for the current player as determined by the latest game state.
     */
    public void makeMove(Set<Move> moves) {
        if (lastGameState == null) return;
        Totem currentTotem = lastGameState.currentPlayerTotem();
        if (currentTotem == null) return;

        String currentPlayerName = totemToName.get(currentTotem);
        if (currentPlayerName == null) return;

        LocalVirtualClient client = clientsByName.get(currentPlayerName);
        if (client != null) {
            client.processRequest(new MoveRequest(moves));
        }
    }

    public GameDTO getLastGameState() {
        return lastGameState;
    }

    public Map<Totem, String> getTotemToName() {
        return Collections.unmodifiableMap(totemToName);
    }

    // Called by the primary client only; updates game state then forwards to TUI
    private void handlePrimaryResponse(ServerResponse response) {
        if (response instanceof GameUpdate update) {
            GameDTO dto = update.getGame();
            lastGameState = dto;
            updateTotemMap(dto);
        }
        tuiCallback.accept(response);
    }

    private void updateTotemMap(GameDTO dto) {
        for (PlayerDTO player : dto.players()) {
            if (player.totem() != null && player.name() != null) {
                totemToName.put(player.totem(), player.name());
            }
        }
    }
}
