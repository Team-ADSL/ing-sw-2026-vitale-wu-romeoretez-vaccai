package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.network.VirtualClient;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.requests.ClientDisconnected;
import org.example.shared.network.requests.EnterGameRequest;
import org.example.shared.network.requests.StartGameRequest;

import java.util.Optional;

public class LobbyState extends ControllerState {
    private boolean readyToStart;

    public LobbyState(Game game, GameController context) {
        super(game, context);
        readyToStart = false;
    }

    @Override
    public ControllerState onEntry() {
        getGame().sendUpdateLobby();
        return this;
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        if(getGame().getPlayers().size() < getGame().getNumPlayer()){
            getGame().addVirtualClient(virtualClient);
            getGame().getPlayers().add(new Player(virtualClient.getClientUsername()));
            virtualClient.setGameController(getContext());
            getGame().sendUpdateLobby();
        } else {
            throw new InvalidRequestException("The lobby is full");
        }
        setNextState(calcNextState());
    }

    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws InvalidRequestException {
        getGame().removeVirtualClient(virtualClient);
        Optional<Player> reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(virtualClient.getClientUsername()))
                .findFirst();
        if(reqPlayer.isEmpty()){
            throw new InvalidRequestException("Player not in current game");
        }
        getGame().getPlayers().remove(reqPlayer.get());
        virtualClient.setGameController(null);

        if(getGame().getPlayers().isEmpty()){
            getGame().sendEndGameResults(null);
        } else {
            getGame().sendUpdateLobby();
        }
        setNextState(calcNextState());
    }

    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        if(getGame().getPlayers().size() == getGame().getNumPlayer()){
            readyToStart = true;
        } else {
            throw new InvalidRequestException(getGame().getNumPlayer() + " players required to start the game");
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
