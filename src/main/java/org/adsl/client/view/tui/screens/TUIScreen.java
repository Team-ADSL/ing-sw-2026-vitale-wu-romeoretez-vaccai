package org.adsl.client.view.tui.screens;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.*;
import org.adsl.client.view.Screen;
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
 * returning a new {@link TUIScreen} instance triggers a transition.
 */
public abstract class TUIScreen extends Screen<TUIScreen> implements InputEventVisitor {

    protected final TuiTerminal terminal;
    protected String username;
    protected boolean toRender;

    protected TUIScreen(TuiTerminal terminal,
            AppCoordinator appCoordinator,
            String username) {
        this.terminal = terminal;
        this.username = username;
        this.toRender = true;
        super(appCoordinator);
    }

    protected TUIScreen(TuiTerminal terminal,
            AppCoordinator coordinator) {
        this(terminal, coordinator, null);
    }

    protected TUIScreen(TuiTerminal terminal) {
        this(terminal, null, null);
    }

    /**
     * No-arg constructor for screens with no resources (e.g. {@link ExitScreen}).
     */
    protected TUIScreen() {
        this(null, null, null);
    }

    /**
     * Server reported an error: jump back to the {@link LoginScreen} carrying the
     * error message.
     */

    @Override
    public TUIScreen createLoginScreen(LoginNeededEvent e, AppCoordinator appCoordinator){
        return new LoginScreen(terminal, appCoordinator);
    }

    @Override
    public TUIScreen createHomeScreen(HomeUpdateEvent e, AppCoordinator appCoordinator){
        return new HomeScreen(terminal, appCoordinator, username, e.activeGames());
    }

    @Override
    public TUIScreen createLobbyScreen(LobbyUpdateEvent e, AppCoordinator appCoordinator){
        return new LobbyScreen(terminal, appCoordinator, username, e.gameId(), e.players(),
                e.numPlayersAllowed());
    }

    @Override
    public TUIScreen createGameScreen(GameUpdateEvent e, AppCoordinator appCoordinator){
        return new GameScreen(terminal, appCoordinator, username, e.game());
    }

    @Override
    public TUIScreen createEndGameScreen(EndGameEvent e, AppCoordinator appCoordinator){
        return new EndGameScreen(terminal, appCoordinator, username, e.results());
    }

    @Override
    public TUIScreen createDisconnectedScreen(DisconnectedEvent e, AppCoordinator appCoordinator){
        return new DisconnectedScreen(terminal, appCoordinator, e.message());
    }

    @Override
    public TUIScreen getThis(){
        return this;
    }

    @Override
    public TUIScreen visit(ConfirmEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(SelectEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(NavigateLeftEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(NavigateRightEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(NavigateUpEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(NavigateDownEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(CharInputEvent e) {
        return this;
    }

    @Override
    public TUIScreen visit(BackspaceEvent e) {
        return this;
    }

    // ── Lifecycle / framework hooks ──────────────────────────────────────────

    public TUIScreen handleEvent(ServerEvent event) {
        TUIScreen newScreen = event.accept(this);
        setToRender(true);
        return newScreen;
    }

    public TUIScreen handleEvent(InputEvent event) {
        TUIScreen newScreen = event.accept(this);
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
     * Returns {@code null} to stay on this screen, or a new {@link TUIScreen}
     * instance to redirect immediately (e.g. back navigation, exit).
     */
    public TUIScreen onEnter() throws Exception {
        return null;
    }

    public abstract void render() throws IOException;
}
