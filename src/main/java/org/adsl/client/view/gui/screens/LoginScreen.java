package org.adsl.client.view.gui.screens;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.LoginNeededEvent;

import java.io.IOException;

/**
 * Username entry. Submits via {@link AppCoordinator#createLoginRequest(String)};
 * server replies with {@code HomeUpdateEvent} (default routing) on success or
 * {@link ErrorEvent} on failure (shown in {@link #errorLabel}).
 */
public class LoginScreen extends GUIScreen {

    @FXML private TextField usernameField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button exitButton;

    public LoginScreen(AppCoordinator coordinator) {
        super(coordinator);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load login.fxml", e);
        }
        applyTheme(this.root);
    }

    @FXML
    private void onLogin() {
        String name = usernameField.getText() == null ? "" : usernameField.getText().trim();
        if (name.isEmpty()) {
            errorLabel.setText("Username cannot be empty.");
            return;
        }
        username = name;
        try {
            appCoordinator.createLoginRequest(name);
            loginButton.setDisable(true);
        } catch (Exception ex) {
            errorLabel.setText("Login failed: " + ex.getMessage());
        }
    }

    @FXML
    private void onExit() {
        try {
            appCoordinator.disconnect();
        } catch (Exception ignored) {}
        javafx.application.Platform.exit();
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        if (errorLabel != null) errorLabel.setText(e.message());
        if (loginButton != null) loginButton.setDisable(false);
        return this;
    }

    /** Stay on the form if the server re-asks for login. */
    @Override
    public GUIScreen visit(LoginNeededEvent e) {
        if (loginButton != null) loginButton.setDisable(false);
        return this;
    }
}
