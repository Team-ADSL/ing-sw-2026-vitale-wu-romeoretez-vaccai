package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.network.requests.TotemPickingRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manual state in which each player picks their totem colour before the game
 * board is initialised.
 * <p>
 * Broadcasts the list of still-available totems after each pick. Once every
 * player has chosen a totem the state transitions to {@link InitGameState}.
 * </p>
 * <p>
 * If a player disconnects while totem picking is in progress the base-class
 * {@code visit(ClientDisconnected)} sets {@code toStop = true}; the overridden
 * {@link #calcNextState()} detects this and transitions to {@link RecoverState}
 * instead, waiting for all players to reconnect before resuming.
 * </p>
 */
public class TotemPickingState extends ControllerState {

    private List<Totem> totemToPick;
    private int numPicked;

    public TotemPickingState(Game game, GameController context) {
        super(game, context);
    }

    /**
     * Computes which totems are still available (accounting for players that
     * may already have a colour set after recovery), broadcasts the available
     * list, and persists the game.
     *
     * @return the next state, computed via {@link #calcNextState()}
     * @throws ServerException never thrown directly, declared for override compatibility
     */
    @Override
    public ControllerState onEntry() throws ServerException {
        Set<Totem> totemInUse = getGame().getPlayers().stream()
                .map(Player::getColor)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        numPicked = totemInUse.size();
        totemToPick = Arrays.stream(Totem.values())
                .filter(totem -> !totemInUse.contains(totem))
                .collect(Collectors.toList());
        getGame().sendTotemAvailable(totemToPick, "[TOTEM PICKING] Waiting for players to pick a totem.");
        getContext().getPersistenceManager().updateGame(getGame());
        setNextState(calcNextState());
        return getNextState();
    }

    /**
     * Assigns the requested totem to the requesting player.
     *
     * @param req           the totem picking request
     * @param virtualClient the client sending the request
     * @throws ServerException if the client has no username, is not in this
     *                          game, has already picked a totem, or the
     *                          requested totem is no longer available
     */
    @Override
    public void visit(TotemPickingRequest req, VirtualClient virtualClient) throws ServerException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("[TOTEM PICKING] Virtual client has no username associated.");
        }
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("Virtual client has no username associated.");
        }
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
                .findFirst()
                .orElseThrow(() -> new ServerException("Player not in current game"));

        if(reqPlayer.getColor() != null){
            throw new ServerException("[TOTEM PICKING] Player already picked a totem.");
        }
        if(!totemToPick.contains(req.getTotem())){
            throw new ServerException("[TOTEM PICKING] Totem already picked.");
        }
        reqPlayer.setColor(req.getTotem());
        totemToPick.remove(req.getTotem());
        numPicked++;

        String log = "[TOTEM PICKING] " + virtualClient.getClientUsername().get() + " has picked "
                + req.getTotem();
        System.out.println(log);
        getGame().sendTotemAvailable(totemToPick, log);
        setNextState(calcNextState());
        getContext().getPersistenceManager().updateGame(getGame());
    }

    /**
     * @return a {@link RecoverState} if a player disconnected during picking,
     *         a new {@link InitGameState} if every player has picked a totem,
     *         otherwise {@code this}
     */
    @Override
    public ControllerState calcNextState(){
        if (isToStop()) {
            return new RecoverState(getGame(), getContext());
        }
        if(numPicked == getGame().getNumPlayer()){
            return new InitGameState(getGame(), getContext());
        } else {
            return this;
        }
    }
}
