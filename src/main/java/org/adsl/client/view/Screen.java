package org.adsl.client.view;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.*;
import org.adsl.shared.network.responses.TotemAvailableUpdate;

public abstract class Screen<S extends Screen<S>> implements EventVisitor<S> {
    protected final AppCoordinator appCoordinator;
    protected String error;

    public Screen(AppCoordinator appCoordinator){
        this.appCoordinator = appCoordinator;
        this.error = null;
    }

    public abstract S createLoginScreen(LoginNeededEvent e, AppCoordinator appCoordinator);
    public abstract S createHomeScreen(HomeUpdateEvent e, AppCoordinator appCoordinator);
    public abstract S createLobbyScreen(LobbyUpdateEvent e, AppCoordinator appCoordinator);
    public abstract S createTotemPickingScreen(TotemAvailableEvent e, AppCoordinator appCoordinator);
    public abstract S createGameScreen(GameUpdateEvent e, AppCoordinator appCoordinator);
    public abstract S createEndGameScreen(EndGameEvent e, AppCoordinator appCoordinator);
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

    public abstract S getThis();

    @Override
    public S visit(DisconnectedEvent e) {
        return createDisconnectedScreen(e, appCoordinator);
    }
}
