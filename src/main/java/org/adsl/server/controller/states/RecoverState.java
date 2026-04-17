package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.controller.StateFactory;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.network.requests.EnterGameRequest;


public class RecoverState extends ControllerState {
    public RecoverState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws ServerException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new ServerException("Virtual client has no username associated.");
        }
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
                .findFirst()
                .orElseThrow(() -> new ServerException("Player not in current game."));

        if(reqPlayer.isActive()){
            throw new ServerException("Player already connected.");
        }

        reqPlayer.setActive(true);
        getGame().addVirtualClient(virtualClient);
        virtualClient.setGameId(getGame().getGameId());
        setNextState(calcNextState());
        getGame().sendUpdateLobby();
    }

    @Override
    public ControllerState calcNextState() {
        int activePlayers = (int)getGame().getPlayers().stream()
                .filter(Player::isActive)
                .count();
        if(activePlayers == getGame().getNumPlayer()){
            getGame().sendUpdateGame();
            getGame().addObserver(getContext().getPersistenceManager());
            return StateFactory.recover(getGame(), getContext());
        } else {
            return this;
        }
    }
}
