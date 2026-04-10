package org.example.client;

import org.example.client.network.ServerConnection;
import org.example.client.view.GameUI;
import org.example.client.exceptions.InvalidResponseException;
import org.example.shared.network.requests.*;
import org.example.shared.network.responses.*;
import org.example.shared.utils.Move;

import java.util.Set;

// Mediator between Network and View. Send response to UI and request to Network.
public class AppCoordinator implements ResponseVisitor{
    private final GameUI gameUI;
    private final ServerConnection serverConnection;
    private int gameId;

    public AppCoordinator(GameUI gameUI, ServerConnection serverConnection) {
        this.gameUI = gameUI;
        this.serverConnection = serverConnection;
        this.gameId = 0;
    }

    // Handle ServerResponse
    @Override
    public void visit(SetUsername response) throws InvalidResponseException {
        gameUI.showUsernameField();
    }
    @Override
    public void visit(HomeUpdate response) throws InvalidResponseException {
        gameUI.onHomeUpdate(response.getActiveGames());
    }
    @Override
    public void visit(LobbyUpdate response) throws InvalidResponseException {
        gameUI.onLobbyUpdate(response.getPlayers());
    }
    @Override
    public void visit(GameUpdate response) throws InvalidResponseException {
        gameUI.onGameUpdate(response.getGame());
    }
    @Override
    public void visit(GameEnded response) throws InvalidResponseException {
        gameUI.onEndGame(response.getResults());
    }
    @Override
    public void visit(ErrorResponse response) throws InvalidResponseException {
        gameUI.onErrorReceived(response.getMessage());
    }
    @Override
    public void visit(ServerDisconnected response) throws InvalidResponseException {
        gameUI.onServerDisconnected();
    }


    // Creating and forwarding ClientRequest to the Server
    public void connectRequest() throws Exception {
        ClientRequest clientRequest = new ClientConnection();
        serverConnection.sendRequest(clientRequest);
    }
    public void setUsernameRequest(String username) throws Exception {
        ClientRequest clientRequest = new SetUsernameRequest(username);
        serverConnection.sendRequest(clientRequest);
    }
    public void createGameRequest(int numPlayer) throws Exception {
        ClientRequest clientRequest = new CreateGameRequest(numPlayer);
        serverConnection.sendRequest(clientRequest);
    }
    public void enterGameRequest() throws Exception {
        ClientRequest clientRequest = new EnterGameRequest(gameId);
        serverConnection.sendRequest(clientRequest);
    }
    public void startGameRequest() throws Exception {
        ClientRequest clientRequest = new StartGameRequest(gameId);
        serverConnection.sendRequest(clientRequest);
    }
    public void makeMoveRequest(Set<Move> moves) throws Exception {
        ClientRequest clientRequest = new MakeMoveRequest(gameId, moves);
        serverConnection.sendRequest(clientRequest);
    }
    public void disconnect() throws Exception {
        ClientRequest clientRequest = new ClientDisconnected(gameId);
        serverConnection.sendRequest(clientRequest);
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }
}
