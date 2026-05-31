package org.adsl.client.view.gui.screens;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.util.Duration;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.ErrorEvent;
import org.adsl.client.serverEvents.LoginNeededEvent;
import org.adsl.client.view.gui.ImageCatalog;

import java.io.IOException;

/**
 * Username entry. Submits via {@link AppCoordinator#createLoginRequest(String)};
 * server replies with {@code HomeUpdateEvent} (default routing) on success or
 * {@link ErrorEvent} on failure (shown in {@link #errorLabel}).
 */
public class LoginScreen extends GUIScreen {

    /** Intro fade plays only on the first login screen of the process, not on
     *  every return to it (e.g. after logout/relogin). */
    private static boolean introShown = false;

    @FXML private TextField usernameField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button exitButton;
    @FXML private ImageView logoImage;

    public LoginScreen(AppCoordinator coordinator) {
        super(coordinator);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setController(this);
            this.root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load login.fxml", e);
        }
        if (logoImage != null) {
            logoImage.setImage(ImageCatalog.load("/assets/general/mesos_logo_white.png"));
        }
        applyTheme(this.root);
        installKeyboardNav();
        playIntroFade();
    }

    /**
     * Arrow-key navigation mirroring HomeScreen. The username field starts
     * focused (cursor ready to type); the first arrow press moves focus onto a
     * button, which the {@code .button:focused} theme rule outlines in white.
     * Layout is: usernameField on top, then [Login | Close] side by side.
     *   field  DOWN  → Login
     *   Login  UP    → field, RIGHT → Close, ENTER → submit
     *   Close  UP    → field, LEFT  → Login, ENTER → close
     */
    private void installKeyboardNav() {
        // Focus the username field as soon as the screen is shown.
        usernameField.sceneProperty().addListener((_, _, scene) -> {
            if (scene != null) Platform.runLater(usernameField::requestFocus);
        });

        usernameField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DOWN) {
                loginButton.requestFocus();
                e.consume();
            }
        });

        loginButton.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP    -> { usernameField.requestFocus(); e.consume(); }
                case RIGHT -> { exitButton.requestFocus();    e.consume(); }
                case ENTER -> { onLogin();                    e.consume(); }
                default    -> { /* ignore */ }
            }
        });

        exitButton.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case UP   -> { usernameField.requestFocus(); e.consume(); }
                case LEFT -> { loginButton.requestFocus();   e.consume(); }
                case ENTER -> { onExit();                    e.consume(); }
                default   -> { /* ignore */ }
            }
        });
    }

    private void playIntroFade() {
        if (root == null || introShown) return;
        introShown = true;
        root.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(900), root);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
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
        appCoordinator.requestShutdown();
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
