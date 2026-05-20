package org.adsl.utils.fakes;

import org.adsl.server.persistence.GameDAO;
import org.adsl.shared.model.DBRecord;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FakeGameDAO implements GameDAO {
    private int idCounter = 1;
    public final List<Integer> createdMatches = new ArrayList<>();
    public final List<Integer> deletedMatches = new ArrayList<>();

    @Override
    public int createMatch() {
        int id = idCounter++;
        createdMatches.add(id);
        return id;
    }

    @Override public void deleteMatch(int id) { deletedMatches.add(id); }
    @Override public void saveMatch(int gameId, int playerCount, List<String> nicknames, List<Integer> scores) throws SQLException {}
    @Override public List<DBRecord> getLeaderboard(int playerCount) throws SQLException { return List.of(); }
    @Override public void setInitialCounter(int i) {}
}