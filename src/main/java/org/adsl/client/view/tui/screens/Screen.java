package org.adsl.client.view.tui.screens;

import com.googlecode.lanterna.gui2.WindowBasedTextGUI;
import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.*;

import java.io.IOException;

/**
 * Base abstract class for all TUI screens. Centralises the attributes and
 * visit defaults shared by every screen so concrete subclasses only need to
 * override what differs.
 * <p>
 * Each concrete screen represents one phase of interaction in the TUI state
 * machine. Server responses and key presses are both delivered as
 * {@link Event} objects and dispatched through the visitor methods inherited
 * from {@link EventVisitor}: returning {@code this} keeps the current screen,
 * returning a new {@link Screen} instance triggers a transition.
 */
public abstract class Screen implements EventVisitor {

    protected final com.googlecode.lanterna.screen.Screen terminal;
    protected final WindowBasedTextGUI gui;
    protected final AppCoordinator coordinator;
    protected String username;
    protected boolean toRender;
    protected String error;

    protected Screen(com.googlecode.lanterna.screen.Screen terminal,
                     WindowBasedTextGUI gui,
                     AppCoordinator coordinator,
                     String username) {
        this.terminal = terminal;
        this.gui = gui;
        this.coordinator = coordinator;
        this.username = username;
        this.toRender = true;
        this.error = null;
    }

    protected Screen(com.googlecode.lanterna.screen.Screen terminal,
                     WindowBasedTextGUI gui,
                     AppCoordinator coordinator) {
        this(terminal, gui, coordinator, null);
    }

    protected Screen(com.googlecode.lanterna.screen.Screen terminal) {
        this(terminal, null, null, null);
    }

    /** No-arg constructor for screens with no resources (e.g. {@link ExitScreen}). */
    protected Screen() {
        this(null, null, null, null);
    }

    // ── Server event defaults ────────────────────────────────────────────────

    /** Server asks the client to (re-)authenticate: jump to {@link LoginScreen}. */
    @Override public Screen visit(LoginNeededEvent e) {
        return new LoginScreen(terminal, gui, coordinator);
    }

    /** Server published the home view: jump to {@link HomeScreen} with the latest active games. */
    @Override public Screen visit(HomeUpdateEvent e) {
        return new HomeScreen(terminal, gui, coordinator, username, e.getActiveGames());
    }

    /** Lobby update is screen-specific (HomeScreen and LobbyScreen own it). */
    @Override
    public Screen visit(LobbyUpdateEvent e) {
        return new LobbyScreen(terminal, gui, coordinator, username, e.getPlayers(), 5);//TODO: add attribute totalPlayers in LobbyUpdate
    }

    /** Game update is screen-specific (LobbyScreen and GameScreen own it). */
    @Override
    public Screen visit(GameUpdateEvent e) {
        return new GameScreen(terminal, gui, coordinator, username, e.getGame());
    }

    /** Game ended: jump to {@link EndGameScreen} with the final results. */
    @Override public Screen visit(EndGameEvent e) {
        return new EndGameScreen(terminal, e.getResults());
    }

    /** Server reported an error: jump back to the {@link LoginScreen} carrying the error message. */
    @Override
    public Screen visit(ErrorEvent e) {
        error = e.getMessage();
        return this;
    }

    @Override
    public Screen visit(DisconnectedEvent e) {
        return new DisconnectedScreen(terminal, gui, coordinator, e.getMessage());
    }

    // ── Input event defaults (unhandled keep current screen) ─────────────────

    @Override public Screen visit(ConfirmEvent e)       { return this; }
    @Override public Screen visit(SelectEvent e)        { return this; }
    @Override public Screen visit(NavigateLeftEvent e)  { return this; }
    @Override public Screen visit(NavigateRightEvent e) { return this; }
    @Override public Screen visit(NavigateUpEvent e)    { return this; }
    @Override public Screen visit(NavigateDownEvent e)  { return this; }
    @Override public Screen visit(CharInputEvent e)     { return this; }

    // ── Lifecycle / framework hooks ──────────────────────────────────────────

    public Screen handleEvent(Event event) {
        Screen newScreen = event.accept(this);
        setToRender(true);
        return newScreen;
    }

    public boolean isToRender() {
        return toRender;
    }

    public void setToRender(boolean value) {
        this.toRender = value;
    }

    /**
     * Called once when the TUI transitions to this screen.
     * Returns {@code null} to stay on this screen, or a new {@link Screen}
     * instance to redirect immediately (e.g. back navigation, exit).
     */
    public Screen onEnter() throws Exception {
        return null;
    }

    public abstract void render() throws IOException;
}
