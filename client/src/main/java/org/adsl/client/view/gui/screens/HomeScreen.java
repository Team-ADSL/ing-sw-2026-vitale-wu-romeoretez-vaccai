package org.adsl.client.view.gui.screens;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.HomeUpdateEvent;
import org.adsl.client.view.gui.FloatingLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI home screen. Displays the list of open games and lets the player create
 * a new game (2–5 players) or join an existing one. Updates in-place when
 * {@code HomeUpdateEvent} arrives. A floating log overlay shows server messages.
 */
public class HomeScreen extends GUIScreen {

    @FXML private StackPane rootStack;
    @FXML private Label welcomeLabel;
    @FXML private Label errorLabel;
    @FXML private ListView<Integer> gamesList;
    @FXML private VBox logBox;

    private List<Integer> activeGames;
    private FloatingLog floatingLog;

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
        applyTheme(this.root);

        floatingLog = new FloatingLog("Home log");
        logBox.getChildren().setAll(floatingLog.getFloatingNode());
        rootStack.getChildren().add(floatingLog.getFullPanel());
    }

    @FXML private void onCreate2() { create(2); }
    @FXML private void onCreate3() { create(3); }
    @FXML private void onCreate4() { create(4); }
    @FXML private void onCreate5() { create(5); }

    private void create(int n) {
        errorLabel.setText("");
        try {
            appCoordinator.createGameRequest(n);
        } catch (Exception ex) {
            errorLabel.setText("Failed to create game: " + ex.getMessage());
        }
    }

    @FXML
    private void onJoinSelected() {
        errorLabel.setText("");
        Integer id = gamesList.getSelectionModel().getSelectedItem();
        if (id == null) {
            errorLabel.setText("Select a game to join.");
            return;
        }
        try {
            appCoordinator.enterGameRequest(id);
        } catch (Exception ex) {
            errorLabel.setText("Failed to join: " + ex.getMessage());
        }
    }

    @FXML
    private void onLogout() {
        errorLabel.setText("");
        try {
            appCoordinator.createLogoutRequest();
        } catch (Exception ex) {
            errorLabel.setText("Logout failed: " + ex.getMessage());
        }
    }

    @Override
    public GUIScreen visit(HomeUpdateEvent e) {
        List<Integer> incoming = e.activeGames();
        this.activeGames = incoming != null ? new ArrayList<>(incoming) : new ArrayList<>();
        gamesList.setItems(FXCollections.observableArrayList(this.activeGames));
        if (e.message() != null && !e.message().isBlank()) floatingLog.append(e.message());
        return this;
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        if (errorLabel != null) errorLabel.setText(e.message());
        return this;
    }
}
