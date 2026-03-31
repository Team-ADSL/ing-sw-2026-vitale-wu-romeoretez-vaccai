package org.example.client.view;

import org.example.shared.model.DatasourceDTO;
import org.example.shared.model.MatchResult;

import java.util.List;

public interface ClientListener {
    void onDataReceived(DatasourceDTO message);
    void onEndGame(List<MatchResult> matchResults);
    void onErrorReceived(String error);
    void onDisconnected();
}
