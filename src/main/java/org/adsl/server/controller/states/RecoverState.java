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
    public ControllerState onEntry(){
        getGame().sendUpdateLobby();
        setToStop(false);
        return this;
    }

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
        System.out.println("[LOBBY] Player " + virtualClient.getClientUsername().get()
                + " connected.");
        getGame().sendUpdateLobby();
        setNextState(calcNextState());
    }

    @Override
    public ControllerState calcNextState() {
        int activePlayers = (int)getGame().getPlayers().stream()
                .filter(Player::isActive)
                .count();
        if(activePlayers == getGame().getNumPlayer()){
            getGame().sendUpdateGame();
            getGame().addObserver(getContext().getPersistenceManager());
            System.out.println("[LOBBY] Starting game...");
            return StateFactory.recover(getGame(), getContext());
        } else {
            return this;
        }
    }
}
