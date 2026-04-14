package org.adsl.client.view;

import org.adsl.shared.model.GameDTO;
import org.adsl.shared.model.MatchResult;

import java.util.List;

public interface GameUI {
    void showUsernameField();
    void onHomeUpdate(List<Integer> activeGames);
    void onLobbyUpdate(List<String> players);
    void onGameUpdate(GameDTO game);
    void onEndGame(List<MatchResult> matchResults);
    void onErrorReceived(String error);
    void onServerDisconnected();
    void start();
}