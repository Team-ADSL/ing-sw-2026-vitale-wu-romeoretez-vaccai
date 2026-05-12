package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.exceptions.HostDisconnectedException;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.network.VirtualClient;
import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.network.requests.*;

public class LobbyState extends ControllerState {
    private boolean readyToStart;
    private final String host;

    public LobbyState(Game game, GameController context, String host) {
        super(game, context);
        this.host = host;
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
            String log = "[LOBBY] Player " + virtualClient.getClientUsername().get() + " connected.";
            System.out.println(log);
            getGame().sendUpdateLobby(log);
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
        String exitLog = "[LOBBY] Player " + virtualClient.getClientUsername().get() + " disconnected.";
        System.out.println(exitLog);

        if(getGame().getPlayers().isEmpty()){
            getGame().sendEndGameResults(null, exitLog);
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

    @Override
    public ControllerState calcNextState() {
        if(readyToStart){
            System.out.println("[LOBBY] Starting game...");
            return new TotemPickingState(getGame(), getContext());
        } else {
            return this;
        }
    }
}
