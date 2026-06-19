package org.adsl.client.view.gui.screens;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.LobbyUpdateEvent;
import org.adsl.client.view.gui.FloatingLog;

import java.io.IOException;
import java.util.List;

/**
 * GUI lobby screen. Shows the players who have joined, the required player
 * count, and a start button (visible only to the host). Updates in-place on
 * {@code LobbyUpdateEvent}; transitions to the game screen on the first
 * {@code GameUpdateEvent}.
 */
public class LobbyScreen extends GUIScreen {

    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private ListView<String> playersList;
    @FXML private VBox logBox;

    private int gameId;
    private List<String> players;
    private final int totalPlayers;
    private final FloatingLog floatingLog;

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

        floatingLog = new FloatingLog();
        logBox.getChildren().setAll(floatingLog.getFloatingNode());

        applyTheme(this.root);
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
        errorLabel.setText("");
        try {
            appCoordinator.startGameRequest();
        } catch (Exception ex) {
            errorLabel.setText("Start failed: " + ex.getMessage());
        }
    }

    @FXML
    private void onLeave() {
        errorLabel.setText("");
        try {
            appCoordinator.createExitLobbyRequest();
        } catch (Exception ex) {
            errorLabel.setText("Leave failed: " + ex.getMessage());
        }
    }

    @Override
    public GUIScreen visit(LobbyUpdateEvent e) {
        this.gameId = e.gameId();
        this.players = e.players() != null ? e.players() : List.of();
        refresh();
        if (e.message() != null && !e.message().isBlank()) floatingLog.append(e.message());
        return this;
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        if (errorLabel != null) errorLabel.setText(e.message());
        return this;
    }
}
