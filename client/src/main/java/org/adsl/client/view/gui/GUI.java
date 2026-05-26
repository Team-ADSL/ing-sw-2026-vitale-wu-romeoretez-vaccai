package org.adsl.client.view.gui;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.EventVisitor;
import org.adsl.client.serverEvents.ServerEvent;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.gui.screens.ConnectingScreen;
import org.adsl.client.view.gui.screens.GUIScreen;

import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final double MIN_W = 800;
    private static final double MIN_H = 540;

    private AppCoordinator coordinator;
    private Stage stage;
    private GUIScreen currentScreen;
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private boolean keyboardNavMode = false;

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
            // Take exit handling away from FX: on macOS the X-button triggers
            // a native [NSWindow _close] cascade that fires resignKeyWindow
            // notifications into Glass's observer AFTER FX has already
            // detached the main thread's JNI env, crashing libglass with
            // SIGSEGV. We consume the close event below (sidesteps the
            // cascade) and halt the process ourselves from the cleanup
            // thread, so implicit-exit is no longer in play.
            Platform.setImplicitExit(false);
            stage = new Stage();
            stage.setTitle(WINDOW_TITLE);
            stage.getIcons().add(ImageCatalog.load("/assets/general/app_icon.png"));
            stage.setWidth(WINDOW_W);
            stage.setHeight(WINDOW_H);
            stage.setMinWidth(MIN_W);
            stage.setMinHeight(MIN_H);
            stage.setResizable(true);
            stage.setOnCloseRequest(ev -> {
                ev.consume();
                shutdown();
            });

            currentScreen = new ConnectingScreen(coordinator);
            Scene scene = new Scene(currentScreen.getRoot(), WINDOW_W, WINDOW_H);
            installFocusVisibleBehavior(scene);
            stage.setScene(scene);
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
        // Idempotent: setOnCloseRequest may fire alongside the JVM shutdown
        // hook in App.java, both ending up here.
        if (!shuttingDown.compareAndSet(false, true)) return;
        System.out.println("[GUI] Shutdown initiated.");

        // Cleanup off the calling thread. coordinator.disconnect() does a
        // synchronous out.println() on the socket; if the write blocks (server
        // hung, half-closed socket, full send buffer) we must NOT freeze the
        // caller — on macOS the close-spinner depends on the process actually
        // terminating, and freezing the JavaFX thread keeps it spinning.
        //
        // We do NOT close the stage or call Platform.exit() from here: either
        // would re-enter the native window-close cascade we sidestepped via
        // ev.consume() in setOnCloseRequest, racing AppKit's resignKeyWindow
        // notification against FX's JNI detach. Halt directly once the
        // network is wound down.
        Thread cleanup = new Thread(() -> {
            if (coordinator != null) {
                try { coordinator.disconnect(); } catch (Exception ignored) {}
            }
            Runtime.getRuntime().halt(0);
        }, "gui-shutdown");
        cleanup.setDaemon(true);
        cleanup.start();

        // Safety net: if the cleanup thread blocks on stuck I/O the process
        // would never exit. Halt unconditionally after 3s.
        Thread killer = new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            Runtime.getRuntime().halt(0);
        }, "gui-force-exit");
        killer.setDaemon(true);
        killer.start();
    }

    // ── Focus-visible (keyboard-only focus ring) ─────────────────────────────

    /**
     * Shows the focus ring on buttons only when navigation is keyboard-driven
     * (Tab, arrows). Mouse clicks never trigger the ring.
     */
    private void installFocusVisibleBehavior(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, _ -> {
            keyboardNavMode = false;
            Node focused = scene.getFocusOwner();
            if (focused instanceof Button b) b.getStyleClass().remove("keyboard-focused");
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            KeyCode code = e.getCode();
            if (code == KeyCode.TAB || code == KeyCode.UP || code == KeyCode.DOWN
                    || code == KeyCode.LEFT || code == KeyCode.RIGHT) {
                keyboardNavMode = true;
            }
        });
        scene.focusOwnerProperty().addListener((_, old, newOwner) -> {
            if (old instanceof Button b) b.getStyleClass().remove("keyboard-focused");
            if (newOwner instanceof Button b && keyboardNavMode) {
                if (!b.getStyleClass().contains("keyboard-focused")) b.getStyleClass().add("keyboard-focused");
            }
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
        if (currentScreen.isExit()) {
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
            if (currentScreen.isExit()) {
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
