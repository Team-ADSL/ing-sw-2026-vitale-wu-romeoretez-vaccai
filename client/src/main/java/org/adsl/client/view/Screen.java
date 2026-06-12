package org.adsl.client.view;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.*;
import org.adsl.shared.network.responses.TotemAvailableUpdate;

/**
 * Transport-independent base for a single game screen. Implements
 * {@link EventVisitor} so incoming server events are directly dispatched via
 * the visitor pattern: returning {@code this} keeps the current screen,
 * returning a new instance triggers a transition.
 * <p>
 * Concrete screen hierarchies ({@code TUIScreen}, {@code GUIScreen}) extend
 * this class and implement the factory methods to instantiate the correct
 * concrete screen for each event type.
 * </p>
 *
 * @param <S> the concrete screen type (self-bounded to enable fluent return types)
 */
public abstract class Screen<S extends Screen<S>> implements EventVisitor<S> {
    protected final AppCoordinator appCoordinator;
    protected String error;

    /**
     * @param appCoordinator coordinator used to send requests to the server
     */
    public Screen(AppCoordinator appCoordinator){
        this.appCoordinator = appCoordinator;
        this.error = null;
    }

    /**
     * Sentinel flag for terminal screens. The TUI loop stops as soon as the
     * current screen reports {@code true}; the GUI runs its shutdown sequence.
     * Defaults to {@code false}; the ExitScreen variants override it. Lets the
     * runtime detect the terminal state through polymorphism instead of a
     * subtype check.
     */
    public boolean isExit() {
        return false;
    }

    /** @return the login screen to show in response to {@code e} */
    public abstract S createLoginScreen(LoginNeededEvent e, AppCoordinator appCoordinator);
    /** @return the home screen to show in response to {@code e} */
    public abstract S createHomeScreen(HomeUpdateEvent e, AppCoordinator appCoordinator);
    /** @return the lobby screen to show in response to {@code e} */
    public abstract S createLobbyScreen(LobbyUpdateEvent e, AppCoordinator appCoordinator);
    /** @return the totem-picking screen to show in response to {@code e} */
    public abstract S createTotemPickingScreen(TotemAvailableEvent e, AppCoordinator appCoordinator);
    /** @return the in-game screen to show in response to {@code e} */
    public abstract S createGameScreen(GameUpdateEvent e, AppCoordinator appCoordinator);
    /** @return the end-game results screen to show in response to {@code e} */
    public abstract S createEndGameScreen(EndGameEvent e, AppCoordinator appCoordinator);
    /** @return the disconnected screen to show in response to {@code e} */
    public abstract S createDisconnectedScreen(DisconnectedEvent e, AppCoordinator appCoordinator);

    @Override
    public S visit(LoginNeededEvent e) {
        return createLoginScreen(e, appCoordinator);
    }

    @Override
    public S visit(HomeUpdateEvent e) {
        return createHomeScreen(e, appCoordinator);
    }

    @Override
    public S visit(LobbyUpdateEvent e) {
        return createLobbyScreen(e, appCoordinator);
    }

    @Override
    public S visit(TotemAvailableEvent e) {
        return createTotemPickingScreen(e, appCoordinator);
    }

    @Override
    public S visit(GameUpdateEvent e) {
        return createGameScreen(e, appCoordinator);
    }

    @Override
    public S visit(EndGameEvent e) {
        return createEndGameScreen(e, appCoordinator);
    }

    @Override
    public S visit(ErrorEvent e) {
        error = e.message();
        return getThis();
    }

    /** @return this screen, typed as {@code S} (workaround for unchecked {@code this} casts in generic self-types) */
    public abstract S getThis();

    @Override
    public S visit(DisconnectedEvent e) {
        return createDisconnectedScreen(e, appCoordinator);
    }

    @Override
    public S visit(EventsTriggeredEvent e) {
        return getThis();
    }

    @Override
    public S visit(GameLogRestoreEvent e) {
        return getThis();
    }
}
