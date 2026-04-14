package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.exceptions.GameException;
import org.adsl.shared.network.requests.ClientDisconnected;
import org.adsl.shared.network.requests.EnterGameRequest;
import org.adsl.shared.network.requests.StartGameRequest;


public class LobbyState extends ControllerState {
    private boolean readyToStart;

    public LobbyState(Game game, GameController context) {
        super(game, context);
        readyToStart = false;
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws GameException {
        if(getGame().getPlayers().size() < getGame().getNumPlayer()){
            if(virtualClient.getClientUsername().isEmpty()){
                throw new GameException("Virtual client has no username associated.");
            }
            getGame().getPlayers().add(new Player(virtualClient.getClientUsername().get()));
            getGame().addVirtualClient(virtualClient);
            virtualClient.setGameId(getGame().getGameId());
            getGame().sendUpdateLobby();
        } else {
            throw new GameException("The lobby is full");
        }
        setNextState(calcNextState());
    }

    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws GameException {
        if(virtualClient.getClientUsername().isEmpty()){
            throw new GameException("Virtual client has no username associated.");
        }
        Player reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername().get()))
                .findFirst()
                .orElseThrow(()->  new GameException("Player not in current game"));

        getGame().getPlayers().remove(reqPlayer);
        getGame().removeVirtualClient(virtualClient);
        virtualClient.setGameId(null);

        if(getGame().getPlayers().isEmpty()){
            getGame().sendEndGameResults(null);
        } else {
            getGame().sendUpdateLobby();
        }
        setNextState(calcNextState());
    }

    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws GameException {
        if(getGame().getPlayers().size() == getGame().getNumPlayer()){
            readyToStart = true;
        } else {
            throw new GameException(getGame().getNumPlayer() + " players required to start the game");
        }
        setNextState(calcNextState());
    }

    @Override
    public ControllerState calcNextState() {
        if(readyToStart){
            return new InitGameState(getGame(), getContext());
        } else {
            return this;
        }
    }
}
