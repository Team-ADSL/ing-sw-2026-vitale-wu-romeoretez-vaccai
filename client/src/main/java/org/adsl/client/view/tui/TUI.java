package org.adsl.client.view.tui;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.DisconnectedEvent;
import org.adsl.client.serverEvents.ServerEvent;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.tui.events.*;
import org.adsl.client.view.tui.render.Key;
import org.adsl.client.view.tui.render.TuiTerminal;
import org.adsl.client.view.tui.screens.ConnectingScreen;
import org.adsl.client.view.tui.screens.TUIScreen;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Text User Interface driven by an event queue and a state/visitor pattern.
 *
 * Server callbacks (network thread) enqueue events into a thread-safe queue.
 * Key presses are translated by {@link #toInputEvent(Key)} and dispatched
 * immediately in the main loop. In both cases the current {@link TUIScreen}
 * receives the event via {@code event.accept(screen)} and returns the next
 * screen (same instance = stay, new instance = transition).
 *
 * Disconnection is handled outside the visitor: {@link #onServerDisconnected()}
 * is called directly by {@link AppCoordinator} and runs a dedicated disconnect
 * flow without involving the screen state machine.
 */
public class TUI extends GameUI {

    private TuiTerminal terminal;

    private TUIScreen currentScreen;
    private final LinkedBlockingQueue<ServerEvent> eventQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Initializes the terminal, shows the connecting screen, requests a
     * connection to the server and runs the main event loop until exit.
     * On any unhandled exception, shows a fatal error screen, then shuts
     * down the terminal and exits the JVM.
     */
    @Override
    public void start() {
        try {
            terminal = new TuiTerminal();
            currentScreen = new ConnectingScreen(terminal, getAppCoordinator());
            getAppCoordinator().connectRequest();
            loop();
        } catch (Exception e) {
            showFatal("Startup error", e);
        } finally {
            shutdown();
            System.exit(0);
        }
    }

    /**
     * Stops the main loop and closes the terminal, restoring the original
     * shell state. Safe to call even if {@link #start()} failed before the
     * terminal was created.
     */
    @Override
    public void shutdown() {
        System.out.println("[TUI] Shutdown initiated.");
        running = false;
        if (terminal != null) {
            terminal.close();
            terminal = null;
        }
    }

    // ── Main loop ─────────────────────────────────────────────────────────────

    private void loop() {
        while (running && !currentScreen.isExit()) {
            // 1. Render the current screen
            try {
                if (currentScreen.isToRender()) {
                    currentScreen.render();
                    currentScreen.setToRender(false);
                }
            } catch (IOException e) {
                showFatal("Render error", e);
                return;
            }

            // 2. Drain server events (enqueued by network callbacks)
            ServerEvent event;
            while ((event = eventQueue.poll()) != null) {
                TUIScreen next = currentScreen.handleEvent(event);
                if (next != currentScreen) {
                    currentScreen = next;
                    if (!transitionTo(currentScreen)) return;
                    break; // re-render before processing further events
                }
            }

            // 3. Translate key press into an input event and dispatch via visitor.
            //    pollInput already has its own short timeout, so no extra sleep.
            try {
                Key key = terminal.pollInput();
                if (key != null) {
                    InputEvent inputEvent = toInputEvent(key);
                    if (inputEvent != null) {
                        TUIScreen next = currentScreen.handleEvent(inputEvent);
                        if (next != currentScreen) {
                            currentScreen = next;
                            if (!transitionTo(currentScreen)) return;
                        }
                    }
                }
            } catch (IOException e) {
                return;
            }
        }
    }

    /**
     * Calls {@link TUIScreen#onEnter()} on {@code screen} and follows any redirect
     * chain returned (each onEnter may return another screen to jump to).
     * Returns {@code true} on success, {@code false} if a fatal error occurred.
     */
    public boolean transitionTo(TUIScreen screen) {
        currentScreen = (TUIScreen) screen;
        try {
            TUIScreen redirect = currentScreen.onEnter();
            while (redirect != null) {
                currentScreen = redirect;
                redirect = currentScreen.onEnter();
            }
        } catch (Exception e) {
            showFatal("TUIScreen transition failed", e);
            return false;
        }
        return true;
    }

    /**
     * Translates a raw key into a semantic input event.
     * Returns {@code null} for keys that carry no meaning in the TUI.
     */
    private InputEvent toInputEvent(Key key) {
        return switch (key.getType()) {
            case ENTER       -> new ConfirmEvent();
            case ARROW_LEFT  -> new NavigateLeftEvent();
            case ARROW_RIGHT -> new NavigateRightEvent();
            case ARROW_UP    -> new NavigateUpEvent();
            case ARROW_DOWN  -> new NavigateDownEvent();
            case BACKSPACE   -> new BackspaceEvent();
            case CHARACTER   -> {
                char c = key.getCharacter();
                yield (c == ' ') ? new SelectEvent() : new CharInputEvent(c);
            }
            default          -> null;
        };
    }

    // ── GameUI callbacks (network thread) ────────────────────────────────────

    /**
     * Enqueues a server event for processing on the main loop thread.
     * Called from the network callback thread.
     *
     * @param event the server event to dispatch
     */
    @Override
    public void dispatch(ServerEvent event){
        eventQueue.add(event);
    }

    /**
     * Enqueues a {@link DisconnectedEvent} so that the screen state machine
     * handles the disconnection through the normal visitor flow. The ping
     * scheduler is stopped to avoid repeated disconnection events.
     */
    @Override
    public void onServerDisconnected() {
        if (terminal == null) return;
        getAppCoordinator().stopPingScheduler();
        eventQueue.add(new DisconnectedEvent(
                "Server disconnected. Check your network connection."));
    }

    // ── Error rendering ───────────────────────────────────────────────────────

    private void showFatal(String label, Exception e) {
        try {
            if (terminal == null) return;
            // Best-effort: write the message in plain ANSI red, wait for any key, then unwind.
            org.adsl.client.view.tui.render.TuiTextGraphics tg = terminal.newTextGraphics();
            terminal.clear();
            tg.setForegroundColor(org.adsl.client.view.tui.render.TuiColor.RED);
            tg.putString(2, 1, "FATAL: " + label + " (" + e.getClass().getSimpleName() + ")");
            tg.putString(2, 2, e.getMessage() != null ? e.getMessage() : "(no message)");
            tg.setForegroundColor(org.adsl.client.view.tui.render.TuiColor.WHITE);
            tg.putString(2, 4, "Press any key to exit.");
            terminal.refresh();
            terminal.pollInput(60_000L);
        } catch (IOException ignored) {}
    }
}
