package org.adsl.client.view;

import org.adsl.client.AppCoordinator;
import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;

import java.util.List;

public interface GameUI {
    void setAppCoordinator(AppCoordinator appCoordinator);
    void showUsernameField();
    void onHomeUpdate(List<Integer> activeGames);
    void onLobbyUpdate(int gameId, List<String> players, int numPlayersAllowed);
    void onGameUpdate(GameDTO game);
    void onEndGame(List<MatchResult> matchResults);
    void onErrorReceived(String error);
    void onServerDisconnected();
    void start();
    void shutdown();
}