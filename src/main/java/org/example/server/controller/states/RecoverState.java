package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.controller.StateFactory;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.network.VirtualClient;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.requests.EnterGameRequest;


public class RecoverState extends ControllerState {
    public RecoverState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry() {
        getGame().sendUpdateLobby();
        return this;
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername()))
                .findFirst()
                .orElseThrow(() -> new InvalidRequestException("Player not in current game"));

        reqPlayer.setActive(true);
        getGame().addVirtualClient(virtualClient);
        virtualClient.setGameController(getContext());
        getGame().sendUpdateLobby();
    }

    @Override
    public ControllerState nextState() {
        int activePlayers = (int)getGame().getPlayers().stream()
                .filter(Player::isActive)
                .count();
        if(activePlayers == getGame().getNumPlayer()){
            setToStop(false);
            return StateFactory.recover(getGame(), getContext());
        } else {
            return this;
        }
    }
}
