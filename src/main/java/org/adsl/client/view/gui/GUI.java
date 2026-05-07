package org.adsl.client.view.gui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.events.DisconnectedEvent;
import org.adsl.client.view.events.EndGameEvent;
import org.adsl.client.view.events.ErrorEvent;
import org.adsl.client.view.events.GameUpdateEvent;
import org.adsl.client.view.events.HomeUpdateEvent;
import org.adsl.client.view.events.LobbyUpdateEvent;
import org.adsl.client.view.events.LoginNeededEvent;
import org.adsl.client.view.events.ServerEvent;
import org.adsl.client.view.gui.screens.ConnectingScreen;
import org.adsl.client.view.gui.screens.ExitScreen;
import org.adsl.client.view.gui.screens.GUIScreen;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;

import java.util.Collections;
import java.util.List;

/**
 * JavaFX implementation of {@link GameUI}, parallel to
 * {@link org.adsl.client.view.tui.TUI}.
 *
 * <p>Network callbacks land on the network thread; this class wraps every
 * delivery in {@link Platform#runLater(Runnable)} so the actual screen
 * transitions and rendering always happen on the JavaFX Application Thread.
 *
 * <p>Screen transitions use the same visitor pattern as the TUI: each
 * server-derived event is dispatched through the current
 * {@link GUIScreen} via {@link ServerEvent#accept(org.adsl.client.view.events.GUIEventVisitor)},
 * the returned screen replaces the current one if different.
 */
public class GUI implements GameUI {

    private static final String WINDOW_TITLE = "MESOS";
    private static final double WINDOW_W = 1280;
    private static final double WINDOW_H = 800;

    private AppCoordinator coordinator;
    private Stage stage;
    private GUIScreen currentScreen;

    @Override
    public void setAppCoordinator(AppCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void start() {
        // Bring up the JavaFX runtime without subclassing Application:
        // Platform.startup runs the supplied task on the JavaFX Application
        // Thread once initialisation is complete, after which Platform.runLater
        // is the standard way to dispatch onto it.
        Platform.startup(() -> {
            stage = new Stage();
            stage.setTitle(WINDOW_TITLE);
            stage.setWidth(WINDOW_W);
            stage.setHeight(WINDOW_H);
            stage.setResizable(true);
            stage.setOnCloseRequest(ev -> shutdown());

            currentScreen = new ConnectingScreen(coordinator);
            stage.setScene(new Scene(currentScreen.getRoot(), WINDOW_W, WINDOW_H));
            runOnEnterChain(currentScreen);
            stage.show();

            try {
                coordinator.connectRequest();
            } catch (Exception e) {
                System.err.println("[GUI] connect request failed: " + e.getMessage());
            }
        });
    }

    @Override
    public void shutdown() {
        Platform.runLater(() -> {
            if (stage != null) stage.close();
            Platform.exit();
        });
    }

    // ── GameUI callbacks (network thread) ────────────────────────────────────

    @Override
    public void showUsernameField() {
        dispatch(new LoginNeededEvent());
    }

    @Override
    public void onHomeUpdate(List<Integer> activeGames) {
        dispatch(new HomeUpdateEvent(activeGames != null ? activeGames : Collections.emptyList()));
    }

    @Override
    public void onLobbyUpdate(int gameId, List<String> players, int numPlayersAllowed) {
        dispatch(new LobbyUpdateEvent(gameId,
                players != null ? players : Collections.emptyList(),
                numPlayersAllowed));
    }

    @Override
    public void onGameUpdate(GameDTO game) {
        dispatch(new GameUpdateEvent(game));
    }

    @Override
    public void onEndGame(List<MatchResult> matchResults) {
        dispatch(new EndGameEvent(matchResults));
    }

    @Override
    public void onErrorReceived(String error) {
        dispatch(new ErrorEvent(error));
    }

    @Override
    public void onServerDisconnected() {
        if (coordinator != null) coordinator.stopPingScheduler();
        dispatch(new DisconnectedEvent("Server disconnected. Check your network connection."));
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Routes the event onto the JavaFX thread, lets the current screen handle
     * it via the visitor, and swaps the scene root when the screen changes.
     */
    private void dispatch(ServerEvent event) {
        Platform.runLater(() -> {
            if (currentScreen == null) return;
            GUIScreen next = event.accept(currentScreen);
            if (next != currentScreen) {
                transitionTo(next);
            }
        });
    }

    private void transitionTo(GUIScreen screen) {
        currentScreen = screen;
        if (currentScreen instanceof ExitScreen) {
            shutdown();
            return;
        }
        if (stage != null && currentScreen.getRoot() != null) {
            stage.setScene(new Scene(currentScreen.getRoot(), stage.getWidth(), stage.getHeight()));
        }
        runOnEnterChain(currentScreen);
    }

    /** Follows {@link GUIScreen#onEnter()} redirects until a stable screen is reached. */
    private void runOnEnterChain(GUIScreen screen) {
        GUIScreen redirect = screen.onEnter();
        while (redirect != null) {
            currentScreen = redirect;
            if (currentScreen instanceof ExitScreen) {
                shutdown();
                return;
            }
            if (stage != null && currentScreen.getRoot() != null) {
                stage.setScene(new Scene(currentScreen.getRoot(), stage.getWidth(), stage.getHeight()));
            }
            redirect = currentScreen.onEnter();
        }
    }
}
