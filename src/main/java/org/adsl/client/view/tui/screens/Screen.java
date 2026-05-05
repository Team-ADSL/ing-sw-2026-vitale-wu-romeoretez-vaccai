package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.view.tui.events.*;
import org.adsl.client.view.tui.render.TuiTerminal;

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

    protected final TuiTerminal terminal;
    protected final AppCoordinator coordinator;
    protected String username;
    protected boolean toRender;
    protected String error;

    protected Screen(TuiTerminal terminal,
            AppCoordinator coordinator,
            String username) {
        this.terminal = terminal;
        this.coordinator = coordinator;
        this.username = username;
        this.toRender = true;
        this.error = null;
    }

    protected Screen(TuiTerminal terminal,
            AppCoordinator coordinator) {
        this(terminal, coordinator, null);
    }

    protected Screen(TuiTerminal terminal) {
        this(terminal, null, null);
    }

    /**
     * No-arg constructor for screens with no resources (e.g. {@link ExitScreen}).
     */
    protected Screen() {
        this(null, null, null);
    }

    // ── Server event defaults ────────────────────────────────────────────────

    /** Server asks the client to (re-)authenticate: jump to {@link LoginScreen}. */
    @Override
    public Screen visit(LoginNeededEvent e) {
        return new LoginScreen(terminal, coordinator);
    }

    /**
     * Server published the home view: jump to {@link HomeScreen} with the latest
     * active games.
     */
    @Override
    public Screen visit(HomeUpdateEvent e) {
        return new HomeScreen(terminal, coordinator, username, e.getActiveGames());
    }

    /** Lobby update is screen-specific (HomeScreen and LobbyScreen own it). */
    @Override
    public Screen visit(LobbyUpdateEvent e) {
        return new LobbyScreen(terminal, coordinator, username, e.getGameId(), e.getPlayers(),
                e.getNumPlayersAllowed());
    }

    /** Game update is screen-specific (LobbyScreen and GameScreen own it). */
    @Override
    public Screen visit(GameUpdateEvent e) {
        return new GameScreen(terminal, coordinator, username, e.getGame());
    }

    /** Game ended: jump to {@link EndGameScreen} with the final results. */
    @Override
    public Screen visit(EndGameEvent e) {
        return new EndGameScreen(terminal, coordinator, username, e.getResults());
    }

    /**
     * Server reported an error: jump back to the {@link LoginScreen} carrying the
     * error message.
     */
    @Override
    public Screen visit(ErrorEvent e) {
        error = e.getMessage();
        return this;
    }

    @Override
    public Screen visit(DisconnectedEvent e) {
        return new DisconnectedScreen(terminal, coordinator, e.getMessage());
    }

    // ── Input event defaults (unhandled keep current screen) ─────────────────

    @Override
    public Screen visit(ConfirmEvent e) {
        return this;
    }

    @Override
    public Screen visit(SelectEvent e) {
        return this;
    }

    @Override
    public Screen visit(NavigateLeftEvent e) {
        return this;
    }

    @Override
    public Screen visit(NavigateRightEvent e) {
        return this;
    }

    @Override
    public Screen visit(NavigateUpEvent e) {
        return this;
    }

    @Override
    public Screen visit(NavigateDownEvent e) {
        return this;
    }

    @Override
    public Screen visit(CharInputEvent e) {
        return this;
    }

    @Override
    public Screen visit(BackspaceEvent e) {
        return this;
    }

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
