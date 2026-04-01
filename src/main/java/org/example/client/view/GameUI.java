package org.example.client.view;

import org.example.shared.model.GameDTO;
import org.example.shared.model.MatchResult;

import java.util.List;

public interface GameUI {
    void showUsernameField();
    void onHomeUpdate(List<Integer> activeGames);
    void onLobbyUpdate(List<String> players);
    void onGameUpdate(GameDTO game);
    void onEndGame(List<MatchResult> matchResults);
    void onErrorReceived(String error);
    void onServerDisconnected();
}