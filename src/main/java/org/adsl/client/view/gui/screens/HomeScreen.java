package org.adsl.client.view.gui.screens;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.events.HomeUpdateEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HomeScreen extends GUIScreen {

    @FXML private Label welcomeLabel;
    @FXML private Label errorLabel;
    @FXML private ListView<Integer> gamesList;

    private List<Integer> activeGames;

    public HomeScreen(AppCoordinator coordinator, String username, List<Integer> activeGames) {
        super(coordinator, username);
        this.activeGames = activeGames != null ? new ArrayList<>(activeGames) : new ArrayList<>();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/home.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load home.fxml", e);
        }
        welcomeLabel.setText("Welcome, " + (username != null ? username : "") + "!");
        gamesList.setItems(FXCollections.observableArrayList(this.activeGames));
    }

    @FXML private void onCreate2() { create(2); }
    @FXML private void onCreate3() { create(3); }
    @FXML private void onCreate4() { create(4); }
    @FXML private void onCreate5() { create(5); }

    private void create(int n) {
        try {
            coordinator.createGameRequest(n);
        } catch (Exception ex) {
            errorLabel.setText("Failed to create game: " + ex.getMessage());
        }
    }

    @FXML
    private void onJoinSelected() {
        Integer id = gamesList.getSelectionModel().getSelectedItem();
        if (id == null) {
            errorLabel.setText("Select a game to join.");
            return;
        }
        try {
            coordinator.enterGameRequest(id);
        } catch (Exception ex) {
            errorLabel.setText("Failed to join: " + ex.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        try {
            coordinator.createLogoutRequest();
        } catch (Exception ex) {
            errorLabel.setText("Logout failed: " + ex.getMessage());
        }
    }

    /** Stay on the screen and refresh the list rather than re-creating it. */
    @Override
    public GUIScreen visit(HomeUpdateEvent e) {
        List<Integer> incoming = e.getActiveGames();
        this.activeGames = incoming != null ? new ArrayList<>(incoming) : new ArrayList<>();
        gamesList.setItems(FXCollections.observableArrayList(this.activeGames));
        return this;
    }
}
