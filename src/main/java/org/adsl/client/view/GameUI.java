package org.adsl.client.view;

import org.adsl.client.AppCoordinator;
import org.adsl.client.serverEvents.*;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;

import java.util.Collections;
import java.util.List;

public abstract class GameUI {
    private AppCoordinator appCoordinator;

    public void showUsernameField() {
        dispatch(new LoginNeededEvent());
    }

    public void onHomeUpdate(List<Integer> activeGames) {
        onHomeUpdate(activeGames, null);
    }

    public void onHomeUpdate(List<Integer> activeGames, String message) {
        dispatch(new HomeUpdateEvent(activeGames != null ? activeGames : Collections.emptyList(),
                message));
    }

    public void onLobbyUpdate(int gameId, List<String> players, int numPlayersAllowed) {
        onLobbyUpdate(gameId, players, numPlayersAllowed, null);
    }

    public void onLobbyUpdate(int gameId, List<String> players, int numPlayersAllowed, String message) {
        dispatch(new LobbyUpdateEvent(gameId,
                players != null ? players : Collections.emptyList(),
                numPlayersAllowed,
                message));
    }

    public void onTotemAvailableUpdate(List<Totem> totemList, String message) {
        dispatch(new TotemAvailableEvent(totemList, message));
    }

    public void onGameUpdate(GameDTO game) {
        dispatch(new GameUpdateEvent(game, null));
    }

    public void onGameUpdate(GameDTO game, String message) {
        dispatch(new GameUpdateEvent(game, message));
    }

    public void onEndGame(List<MatchResult> matchResults) {
        dispatch(new EndGameEvent(matchResults));
    }

    public void onEventTriggered(String eventTitle, String logMessage) {
        dispatch(new EventsTriggeredEvent(eventTitle, logMessage));
    }

    public void onErrorReceived(String error) {
        dispatch(new ErrorEvent(error));
    }

    public void onServerDisconnected() {
        if (appCoordinator != null) appCoordinator.stopPingScheduler();
        dispatch(new DisconnectedEvent("Server disconnected. Check your network connection."));
    }

    public abstract void dispatch(ServerEvent event);
    public abstract void start();
    public abstract void shutdown();

    public AppCoordinator getAppCoordinator() {
        return appCoordinator;
    }

    public void setAppCoordinator(AppCoordinator appCoordinator) {
        this.appCoordinator = appCoordinator;
    }
}