package org.adsl.client.view.gui.screens;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import org.adsl.client.AppCoordinator;

import java.io.IOException;

/**
 * GUI screen shown when the connection to the server is lost. Displays the
 * disconnection message and provides a button to attempt reconnection.
 */
public class DisconnectedScreen extends GUIScreen {

    @FXML private Label messageLabel;
    @FXML private Label errorLabel;

    public DisconnectedScreen(AppCoordinator coordinator, String message) {
        super(coordinator);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/disconnected.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load disconnected.fxml", e);
        }
        messageLabel.setText(message != null ? message : "");
        applyTheme(this.root);
    }

    @FXML
    private void onReconnect() {
        try {
            appCoordinator.reconnect();
        } catch (Exception ex) {
            errorLabel.setText("Could not reconnect: " + ex.getMessage());
        }
    }

    @FXML
    private void onExit() {
        appCoordinator.requestShutdown();
    }
}
