package org.adsl.client.view.tui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.gui2.MultiWindowTextGUI;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorAutoCloseTrigger;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.GameUI;
import org.adsl.client.view.tui.events.*;
import org.adsl.client.view.tui.screens.ConnectingScreen;
import org.adsl.client.view.tui.screens.ExitScreen;
import org.adsl.client.view.tui.screens.Screen;
import org.adsl.client.view.tui.screens.DisconnectedScreen;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Text User Interface driven by an event queue and a state/visitor pattern.
 *
 * Both server callbacks and key presses produce {@link Event} objects.
 * Server callbacks (network thread) enqueue events into a thread-safe queue.
 * Key presses are translated by {@link #toInputEvent(KeyStroke)} and
 * dispatched immediately in the main loop. In both cases the current
 * {@link Screen} receives the event via {@code event.accept(screen)} and
 * returns the next screen (same instance = stay, new instance = transition).
 *
 * Disconnection is handled outside the visitor: {@link #onServerDisconnected()}
 * is called directly by {@link AppCoordinator} and runs a dedicated disconnect
 * flow without involving the screen state machine.
 */
public class TUI implements GameUI {

    private com.googlecode.lanterna.screen.Screen terminal;
    private MultiWindowTextGUI gui;
    private AppCoordinator appCoordinator;

    private Screen currentScreen;
    private final LinkedBlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    @Override
    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void start() {
        try {
            initLanterna();
            currentScreen = new ConnectingScreen(terminal, gui, appCoordinator);
            appCoordinator.connectRequest();
            loop();
        } catch (Exception e) {
            showFatal("Startup error", e);
        } finally {
            shutdown();
        }
    }

    @Override
    public void shutdown() {
        running = false;
        try {
            if (terminal != null) {
                terminal.stopScreen();
                terminal = null;
            }
        } catch (IOException ignored) {}
    }

    // ── Main loop ─────────────────────────────────────────────────────────────

    private void loop() {
        while (running && !(currentScreen instanceof ExitScreen)) {
            // 1. Render the current screen
            try {
                if(currentScreen.isToRender()){
                    currentScreen.render();
                    currentScreen.setToRender(false);
                }
            } catch (IOException e) {
                showFatal("Render error", e);
                return;
            }

            // 2. Drain server events (enqueued by network callbacks)
            Event event;
            while ((event = eventQueue.poll()) != null) {
                if (event instanceof ErrorEvent ee) {
                    flashError(ee.getMessage());
                }
                Screen next = currentScreen.handleEvent(event);
                if (next != currentScreen) {
                    currentScreen = next;
                    if (!transitionTo(currentScreen)) return;
                    break; // re-render before processing further events
                }
            }

            // 3. Translate key press into an input event and dispatch via visitor
            try {
                KeyStroke key = terminal.pollInput();
                if (key != null) {
                    Event inputEvent = toInputEvent(key);
                    if (inputEvent != null) {
                        Screen next = inputEvent.accept(currentScreen);
                        if (next != currentScreen) {
                            currentScreen = next;
                            if (!transitionTo(currentScreen)) return; // TODO: endGame does not imply end session
                        }
                    }
                } else {
                    Thread.sleep(20);
                }
            } catch (IOException | InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Calls {@link Screen#onEnter()} on {@code screen} and follows any redirect
     * chain returned (each onEnter may return another screen to jump to).
     * Returns {@code true} on success, {@code false} if a fatal error occurred.
     */
    private boolean transitionTo(Screen screen) {
        currentScreen = screen;
        try {
            Screen redirect = currentScreen.onEnter();
            while (redirect != null) {
                currentScreen = redirect;
                redirect = currentScreen.onEnter();
            }
        } catch (Exception e) {
            showFatal("Screen transition failed", e);
            return false;
        }
        return true;
    }

    /**
     * Translates a raw Lanterna keystroke into a semantic input event.
     * Returns {@code null} for keys that carry no meaning in the TUI.
     */
    private Event toInputEvent(KeyStroke key) {
        return switch (key.getKeyType()) {
            case Enter      -> new ConfirmEvent();
            case ArrowLeft  -> new NavigateLeftEvent();
            case ArrowRight -> new NavigateRightEvent();
            case ArrowUp    -> new NavigateUpEvent();
            case ArrowDown  -> new NavigateDownEvent();
            case Character  -> {
                Character c = key.getCharacter();
                yield (c != null && c == ' ') ? new SelectEvent() : new CharInputEvent(c);
            }
            default         -> null;
        };
    }

    // ── GameUI callbacks (network thread) ────────────────────────────────────

    @Override
    public void showUsernameField() {
        eventQueue.add(new LoginNeededEvent());
    }

    @Override
    public void onHomeUpdate(List<Integer> activeGames) {
        eventQueue.add(new HomeUpdateEvent(
                activeGames != null ? activeGames : Collections.emptyList()));
    }

    @Override
    public void onLobbyUpdate(List<String> players) {
        eventQueue.add(new LobbyUpdateEvent(
                players != null ? players : Collections.emptyList()));
    }

    @Override
    public void onGameUpdate(GameDTO game) {
        eventQueue.add(new GameUpdateEvent(game));
    }

    @Override
    public void onEndGame(List<MatchResult> results) {
        eventQueue.add(new EndGameEvent(results));
    }

    @Override
    public void onErrorReceived(String error) {
        eventQueue.add(new ErrorEvent(error));
    }

    /**
     * Enqueues a {@link DisconnectedEvent} so that the screen state machine
     * handles the disconnection through the normal visitor flow. The ping
     * scheduler is stopped to avoid repeated disconnection events.
     */
    @Override
    public void onServerDisconnected() {
        if (terminal == null) return;
        appCoordinator.stopPingScheduler();
        eventQueue.add(new DisconnectedEvent(terminal, gui, appCoordinator,
                "Server disconnected. Check your network connection."));
    }

    // ── Error rendering ───────────────────────────────────────────────────────

    private void showFatal(String label, Exception e) {
        try {
            if (terminal == null) return;
            terminal.clear();
            TextGraphics tg = terminal.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.RED);
            tg.putString(2, 1, "FATAL: " + label + " (" + e.getClass().getSimpleName() + ")");
            tg.putString(2, 2, e.getMessage() != null ? e.getMessage() : "(no message)");
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            tg.putString(2, 4, "Press ENTER to exit.");
            terminal.refresh();
            terminal.readInput();
        } catch (IOException ignored) {}
    }

    private void flashError(String msg) {
        try {
            if (terminal == null) return;
            int row = terminal.getTerminalSize().getRows() - 2;
            TextGraphics tg = terminal.newTextGraphics();
            tg.setForegroundColor(TextColor.ANSI.RED);
            tg.putString(2, row, "! ERROR: " + msg);
            tg.setForegroundColor(TextColor.ANSI.WHITE);
            terminal.refresh();
        } catch (IOException ignored) {}
    }

    // ── Lanterna bootstrap ────────────────────────────────────────────────────

    private void initLanterna() throws IOException {
        Terminal lanternaTerminal;
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            SwingTerminalFrame frame = new SwingTerminalFrame("MESOS – Ancient Tribe Strategy",
                    TerminalEmulatorAutoCloseTrigger.CloseOnExitPrivateMode);
            frame.setVisible(true);
            lanternaTerminal = frame;
        } else {
            lanternaTerminal = new DefaultTerminalFactory().createTerminal();
        }
        terminal = new TerminalScreen(lanternaTerminal);
        terminal.startScreen();
        gui = new MultiWindowTextGUI(terminal);
    }
}
