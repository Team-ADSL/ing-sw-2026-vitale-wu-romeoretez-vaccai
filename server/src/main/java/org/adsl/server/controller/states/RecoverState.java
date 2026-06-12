package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.controller.StateFactory;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.network.requests.EnterGameRequest;

/**
 * Manual state that waits for all players to reconnect after a server restart.
 * <p>
 * Players whose {@code isActive} flag is {@code false} (set during recovery)
 * must send an {@link EnterGameRequest} to re-join. Once all expected players
 * are active again the state uses {@link StateFactory} to reconstruct the
 * correct game-phase state and resume play.
 * </p>
 */
public class RecoverState extends ControllerState {
    public RecoverState(Game game, GameController context) {
        super(game, context);
    }

    /**
     * Notifies connected clients of the lobby state and clears the stop flag
     * so the state machine keeps waiting for reconnections.
     *
     * @return {@code this}, since this is a manual state waiting for
     *         {@link EnterGameRequest}s
     */
    @Override
    public ControllerState onEntry(){
        getGame().sendUpdateLobby();
        setToStop(false);
        return this;
    }

    /**
     * Reactivates a previously disconnected player, replays the accumulated
     * game log to their client, and recomputes the next state.
     *
     * @param req           the reconnection request
     * @param virtualClient the reconnecting client
     * @throws ServerException if the client has no username, is not a player
     *                          in this game, or is already marked active
     */
    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws ServerException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("[LOBBY] Virtual client has no username associated.");
        }
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
                .findFirst()
                .orElseThrow(() -> new ServerException("[LOBBY] Player not in current game."));

        if(reqPlayer.isActive()){
            throw new ServerException("[LOBBY] Player already connected.");
        }

        reqPlayer.setActive(true);
        getGame().addVirtualClient(virtualClient);
        virtualClient.setGameId(getGame().getGameId());
        // Replay the accumulated transcript to the reconnecting client only, so a
        // fresh client process recovers the full game log.
        virtualClient.restoreGameLog(getGame().getGameLog());
        String log = "[LOBBY] Player " + virtualClient.getClientUsername().get() + " connected.";
        System.out.println(log);
        getGame().sendUpdateLobby(log);
        setNextState(calcNextState());
    }

    /**
     * @return the state reconstructed by {@link StateFactory#recover} if every
     *         expected player has reconnected, otherwise {@code this}
     */
    @Override
    public ControllerState calcNextState() {
        int activePlayers = (int)getGame().getPlayers().stream()
                .filter(Player::isActive)
                .count();
        if(activePlayers == getGame().getNumPlayer()){
            getGame().addObserver(getContext().getPersistenceManager());
            System.out.println("[LOBBY] Starting game...");
            return StateFactory.recover(getGame(), getContext());
        } else {
            return this;
        }
    }
}
