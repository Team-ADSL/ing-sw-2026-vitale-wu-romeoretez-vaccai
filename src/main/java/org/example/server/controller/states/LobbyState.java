package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.network.VirtualClient;
import org.example.shared.enums.Phase;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.requests.ClientDisconnected;
import org.example.shared.network.requests.ConnectToGameRequest;
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
        getGame().setPhase(Phase.LOBBY);
        return this;
    }

    @Override
    public void visit(ConnectToGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        if(getGame().getPlayers().size() < 5){
            getGame().addVirtualClient(virtualClient);
            getGame().getPlayers().add(new Player(req.getUsername()));
            virtualClient.setGameController(Optional.of(getContext()));
        } else {
            throw new InvalidRequestException("The lobby is full");
        }
    }

    @Override
    public void visit(ClientDisconnected req, VirtualClient virtualClient) throws InvalidRequestException {
        getGame().removeVirtualClient(virtualClient);
        Optional<Player> reqPlayer = getGame().getPlayers().stream()
                .filter(p -> p.getName().equals(req.getUsername()))
                .findFirst();
        if(reqPlayer.isEmpty()){
            throw new InvalidRequestException("Player not in current game");
        }
        getGame().getPlayers().remove(reqPlayer.get());
        virtualClient.setGameController(Optional.empty());

        if(getGame().getPlayers().isEmpty()){
            getGame().notifyEndGame(null);
        }
    }

    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        if(getGame().getPlayers().size() >= 2){
            readyToStart = true;
        } else {
            throw new InvalidRequestException("At least 2 players required to start the game");
        }
    }

    @Override
    public ControllerState nextState() {
        if(readyToStart){
            return new InitGameState(getGame(), getContext());
        } else {
            return this;
        }
    }
}
