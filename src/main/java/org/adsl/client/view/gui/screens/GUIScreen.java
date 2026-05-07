package org.adsl.client.view.gui.screens;

import javafx.scene.Parent;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.events.DisconnectedEvent;
import org.adsl.client.view.events.EndGameEvent;
import org.adsl.client.view.events.ErrorEvent;
import org.adsl.client.view.events.GUIEventVisitor;
import org.adsl.client.view.events.GameUpdateEvent;
import org.adsl.client.view.events.HomeUpdateEvent;
import org.adsl.client.view.events.LobbyUpdateEvent;
import org.adsl.client.view.events.LoginNeededEvent;

/**
 * Base class for every GUI screen. Mirrors {@link org.adsl.client.view.tui.screens.TUIScreen}
 * for the JavaFX side: each concrete screen owns its FXML controller role,
 * exposes the JavaFX {@link Parent} root and reacts to server events via the
 * {@link GUIEventVisitor} contract. Default visit methods route events to the
 * "natural" next screen so concrete screens only override what differs.
 */
public abstract class GUIScreen implements GUIEventVisitor {

    protected final AppCoordinator coordinator;
    protected String username;
    protected String error;
    protected Parent root;

    protected GUIScreen(AppCoordinator coordinator, String username) {
        this.coordinator = coordinator;
        this.username = username;
        this.error = null;
    }

    protected GUIScreen(AppCoordinator coordinator) {
        this(coordinator, null);
    }

    /** JavaFX root node displayed by this screen. */
    public Parent getRoot() {
        return root;
    }

    /**
     * Called once when the GUI transitions to this screen, on the JavaFX
     * Application Thread. Returns {@code null} to stay, or a new
     * {@link GUIScreen} to redirect immediately.
     */
    public GUIScreen onEnter() {
        return null;
    }

    // ── Default server-event routing (mirrors TUIScreen defaults) ──────────────

    @Override
    public GUIScreen visit(LoginNeededEvent e) {
        return new LoginScreen(coordinator);
    }

    @Override
    public GUIScreen visit(HomeUpdateEvent e) {
        return new HomeScreen(coordinator, username, e.getActiveGames());
    }

    @Override
    public GUIScreen visit(LobbyUpdateEvent e) {
        return new LobbyScreen(coordinator, username, e.getGameId(), e.getPlayers(),
                e.getNumPlayersAllowed());
    }

    @Override
    public GUIScreen visit(GameUpdateEvent e) {
        return new GameScreen(coordinator, username, e.getGame());
    }

    @Override
    public GUIScreen visit(EndGameEvent e) {
        return new EndGameScreen(coordinator, username, e.getResults());
    }

    @Override
    public GUIScreen visit(ErrorEvent e) {
        error = e.getMessage();
        return this;
    }

    @Override
    public GUIScreen visit(DisconnectedEvent e) {
        return new DisconnectedScreen(coordinator, e.getMessage());
    }
}
