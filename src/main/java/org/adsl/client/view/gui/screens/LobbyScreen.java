package org.adsl.client.view.gui.screens;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.events.LobbyUpdateEvent;

import java.io.IOException;
import java.util.List;

public class LobbyScreen extends GUIScreen {

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private ListView<String> playersList;

    private int gameId;
    private List<String> players;
    private final int totalPlayers;

    public LobbyScreen(AppCoordinator coordinator,
                       String username,
                       int gameId,
                       List<String> players,
                       int totalPlayers) {
        super(coordinator, username);
        this.gameId = gameId;
        this.players = players != null ? players : List.of();
        this.totalPlayers = totalPlayers;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/lobby.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load lobby.fxml", e);
        }
        refresh();
    }

    private void refresh() {
        titleLabel.setText("MESOS — Game #" + gameId);
        statusLabel.setText(totalPlayers > 0
                ? String.format("(%d / %d players ready)", players.size(), totalPlayers)
                : String.format("(%d players in lobby)", players.size()));
        playersList.setItems(FXCollections.observableArrayList(players));
    }

    @FXML
    private void onStart() {
        try {
            coordinator.startGameRequest();
        } catch (Exception ex) {
            errorLabel.setText("Start failed: " + ex.getMessage());
        }
    }

    @FXML
    private void onLeave() {
        try {
            coordinator.createExitLobbyRequest();
        } catch (Exception ex) {
            errorLabel.setText("Leave failed: " + ex.getMessage());
        }
    }

    @Override
    public GUIScreen visit(LobbyUpdateEvent e) {
        this.gameId = e.getGameId();
        this.players = e.getPlayers() != null ? e.getPlayers() : List.of();
        refresh();
        return this;
    }
}
