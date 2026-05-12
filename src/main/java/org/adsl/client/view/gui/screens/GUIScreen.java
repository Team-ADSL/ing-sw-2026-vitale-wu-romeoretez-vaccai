package org.adsl.client.view.gui.screens;

import javafx.scene.Parent;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.*;
import org.adsl.client.view.Screen;
import org.adsl.shared.network.responses.TotemAvailableUpdate;

/**
 * Base class for every GUI screen. Mirrors {@link org.adsl.client.view.tui.screens.TUIScreen}
 * for the JavaFX side: each concrete screen owns its FXML controller role,
 * exposes the JavaFX {@link Parent} root and reacts to server events via the
 * {@link EventVisitor} contract. Default visit methods route events to the
 * "natural" next screen so concrete screens only override what differs.
 */
public abstract class GUIScreen extends Screen<GUIScreen> {

    protected String username;
    protected String error;
    protected Parent root;

    protected GUIScreen(AppCoordinator coordinator, String username) {
        this.username = username;
        this.error = null;
        super(coordinator);
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
    public GUIScreen createLoginScreen(LoginNeededEvent e, AppCoordinator appCoordinator){
        return new LoginScreen(appCoordinator);
    }

    @Override
    public GUIScreen createHomeScreen(HomeUpdateEvent e, AppCoordinator appCoordinator){
        return new HomeScreen(appCoordinator, username, e.activeGames());
    }

    @Override
    public GUIScreen createLobbyScreen(LobbyUpdateEvent e, AppCoordinator appCoordinator){
        return new LobbyScreen(appCoordinator, username, e.gameId(), e.players(),
                e.numPlayersAllowed());
    }

    @Override
    public GUIScreen createTotemPickingScreen(TotemAvailableEvent e, AppCoordinator appCoordinator){
        return new TotemPickingScreen(appCoordinator, username, e.totemList());
    }

    @Override
    public GUIScreen createGameScreen(GameUpdateEvent e, AppCoordinator appCoordinator){
        return new GameScreen(appCoordinator, username, e.game());
    }

    @Override
    public GUIScreen createEndGameScreen(EndGameEvent e, AppCoordinator appCoordinator){
        return new EndGameScreen(appCoordinator, username, e.results());
    }

    @Override
    public GUIScreen createDisconnectedScreen(DisconnectedEvent e, AppCoordinator appCoordinator){
        return new DisconnectedScreen(appCoordinator, e.message());
    }

    @Override
    public GUIScreen getThis(){
        return this;
    }
}
