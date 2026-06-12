package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.exceptions.HostDisconnectedException;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.network.requests.*;

/**
 * Manual state that manages the pre-game lobby.
 * <p>
 * Accepts {@code EnterGameRequest} until the lobby is full, rejects duplicates,
 * and transitions to {@code TotemPickingState} when the host sends
 * {@code StartGameRequest} with all seats filled. If the host disconnects all
 * remaining players are evicted and a {@code HostDisconnectedException} is
 * propagated to {@code ServerController} to clean up the game entry.
 * </p>
 */
public class LobbyState extends ControllerState {
    private boolean readyToStart;
    private final String host;

    /**
     * @param game    the game model owning the lobby
     * @param context the {@link GameController} that will own this state
     * @param host    the username of the player allowed to start the game
     */
    public LobbyState(Game game, GameController context, String host) {
        super(game, context);
        this.host = host;
        readyToStart = false;
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws ServerException {
        if(getGame().getPlayers().size() == getGame().getNumPlayer()) {
            throw new ServerException("[LOBBY] The lobby is full");
        }
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("[LOBBY] Virtual client has no username associated.");
        }
        String newUser = virtualClient.getClientUsername().get();
        boolean isDuplicate = getGame().getPlayers().stream()
                .anyMatch(p -> p.getName().equals(newUser));
        if(isDuplicate){
            throw new ServerException("[LOBBY] Client already connected.");
        }
        getGame().getPlayers().add(new Player(virtualClient.getClientUsername().get()));
        getGame().addVirtualClient(virtualClient);
        virtualClient.setGameId(getGame().getGameId());

        String log = "[LOBBY] Player " + virtualClient.getClientUsername().get() + " connected.";
        System.out.println(log);
        getGame().sendUpdateLobby(log);
        setNextState(calcNextState());
    }

    @Override
    public void visit(ExitLobbyRequest req, VirtualClient virtualClient) throws ServerException {
        playerExit(virtualClient);
    }

    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws ServerException {
        playerExit(virtualClient);
    }

    /**
     * Removes the player associated with {@code virtualClient} from the lobby,
     * shared by {@code ExitLobbyRequest} and {@code ClientDisconnected} handling.
     * <p>
     * If the lobby becomes empty the game is ended with no results. If the host
     * leaves, every remaining player is evicted and a
     * {@link HostDisconnectedException} is thrown so {@code ServerController}
     * can discard this game. Otherwise the remaining players are notified.
     * </p>
     *
     * @param virtualClient the client leaving the lobby
     * @throws ServerException if the client has no username or is not in this game
     * @throws HostDisconnectedException if the host left the lobby
     */
    public void playerExit(VirtualClient virtualClient){
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("[LOBBY] Virtual client has no username associated.");
        }
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
                .findFirst()
                .orElseThrow(()->  new ServerException("[LOBBY] Player not in current game."));

        getGame().getPlayers().remove(reqPlayer);
        getGame().removeVirtualClient(virtualClient);
        virtualClient.setGameId(null);
        String exitLog = "[LOBBY] Player " + virtualClient.getClientUsername().get() + " disconnected.";
        System.out.println(exitLog);

        if(getGame().getPlayers().isEmpty()){
            getGame().sendEndGameResults(null, null, exitLog);
        } else if(host.equals(virtualClient.getClientUsername().get())) {
            String log = "[LOBBY] Host " + host + " left, every player has been disconnected.";
            getGame().broadcastError(log);
            throw new HostDisconnectedException(log);
        } else {
            getGame().sendUpdateLobby(exitLog);
        }
        setNextState(calcNextState());
    }

    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws ServerException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("[LOBBY] Virtual client has no username associated.");
        }
        if(!virtualClient.getClientUsername().get().equals(host)){
            throw new ServerException("[LOBBY] Only the host can start the game.");
        }
        if(getGame().getPlayers().size() == getGame().getNumPlayer()){
            readyToStart = true;
        } else {
            throw new ServerException("[LOBBY]" + getGame().getNumPlayer() + " players required to start the game");
        }
        setNextState(calcNextState());
    }

    /**
     * @return a new {@link TotemPickingState} if the host has started the game
     *         with all seats filled, otherwise {@code this}
     */
    @Override
    public ControllerState calcNextState() {
        if(readyToStart){
            System.out.println("[LOBBY] Starting game...");
            getGame().addObserver(getContext().getPersistenceManager());
            getGame().setPhase(Phase.TOTEM_PICKING);
            return new TotemPickingState(getGame(), getContext());
        } else {
            return this;
        }
    }
}
