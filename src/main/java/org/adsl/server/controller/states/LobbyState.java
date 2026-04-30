package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.network.requests.*;

public class LobbyState extends ControllerState {
    private boolean readyToStart;

    public LobbyState(Game game, GameController context) {
        super(game, context);
        readyToStart = false;
    }

    @Override
    public void visit(EnterGameRequest req, VirtualClient virtualClient) throws ServerException {
        if(getGame().getPlayers().size() == getGame().getNumPlayer()) {
            throw new ServerException("[LOBBY] The lobby is full");
        } else {
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
            System.out.println("[LOBBY] Player " + virtualClient.getClientUsername().get()
                                + " connected.");
            getGame().sendUpdateLobby();
        }
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
        System.out.println("[LOBBY] Player " + virtualClient.getClientUsername().get()
                + " disconnected.");

        if(getGame().getPlayers().isEmpty()){
            getGame().sendEndGameResults(null);
        } else {
            getGame().sendUpdateLobby();
        }
        setNextState(calcNextState());
    }

    @Override
    public void visit(StartGameRequest req, VirtualClient virtualClient) throws ServerException {
        if(getGame().getPlayers().size() == getGame().getNumPlayer()){
            readyToStart = true;
        } else {
            throw new ServerException("[LOBBY]" + getGame().getNumPlayer() + " players required to start the game");
        }
        setNextState(calcNextState());
    }

    @Override
    public ControllerState calcNextState() {
        if(readyToStart){
            System.out.println("[LOBBY] Starting game...");
            return new InitGameState(getGame(), getContext());
        } else {
            return this;
        }
    }
}
