package org.adsl.client.view.gui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.Event;
import org.adsl.client.serverEvents.EventVisitor;
import org.adsl.client.serverEvents.ServerEvent;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.Screen;
import org.adsl.client.view.gui.screens.ConnectingScreen;
import org.adsl.client.view.gui.screens.ExitScreen;
import org.adsl.client.view.gui.screens.GUIScreen;

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
 * {@link GUIScreen} via {@link ServerEvent#accept(EventVisitor)},
 * the returned screen replaces the current one if different.
 */
public class GUI extends GameUI {

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
            ImageCatalog.loadFonts();
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
        if (coordinator != null) {
            try { coordinator.disconnect(); } catch (Exception ignored) {}
        }
        Platform.runLater(() -> {
            if (stage != null) stage.close();
            Platform.exit();
        });
    }

    // ── GameUI callbacks (network thread) ────────────────────────────────────



    // ── Internal ─────────────────────────────────────────────────────────────

    /**
     * Routes the event onto the JavaFX thread, lets the current screen handle
     * it via the visitor, and swaps the scene root when the screen changes.
     */
    @Override
    public void dispatch(ServerEvent event) {
        Platform.runLater(() -> {
            if (currentScreen == null) return;
            GUIScreen next = event.accept(currentScreen);
            if (next != currentScreen) {
                transitionTo(next);
            }
        });
    }

    public void transitionTo(GUIScreen screen) {
        currentScreen = screen;
        if (currentScreen instanceof ExitScreen) {
            shutdown();
            return;
        }
        swapRoot(currentScreen);
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
            swapRoot(currentScreen);
            redirect = currentScreen.onEnter();
        }
    }

    /**
     * Replaces the {@link Scene}'s root instead of building a new {@link Scene}
     * per transition. Creating a new Scene and calling
     * {@link Stage#setScene(Scene)} is destructive on macOS: it kicks the
     * window out of native fullscreen / split-view because the OS treats it
     * as a fresh window. Keeping a single Scene attached to the Stage and
     * mutating its root preserves window state across screen changes.
     */
    private void swapRoot(GUIScreen screen) {
        if (stage == null || screen.getRoot() == null) return;
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.setRoot(screen.getRoot());
        } else {
            stage.setScene(new Scene(screen.getRoot(), stage.getWidth(), stage.getHeight()));
        }
    }
}
