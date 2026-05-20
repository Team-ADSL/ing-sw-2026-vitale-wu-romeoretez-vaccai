package org.adsl.server.persistence;

import org.adsl.server.db.ConnectionProvider;
import org.adsl.shared.model.DBRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL implementation of {@link GameDAO}.
 * <p>
 * Uses a {@link ConnectionProvider} to obtain connections so that the data
 * source can be swapped in tests. {@link #saveMatch} runs inside a transaction
 * and rolls back automatically on failure. {@link #getLeaderboard} computes
 * a dense rank client-side (cumulative scores per player, ordered descending).
 * </p>
 */
public class SqlGameDAO implements GameDAO{
    private ConnectionProvider connectionProvider;

    public SqlGameDAO(ConnectionProvider connectionProvider){
        this.connectionProvider = connectionProvider;
    }

    // Game creation for the lobby before starting (the number of player is unknown)
    public int createMatch() throws SQLException {
        String sql = "INSERT INTO matches () VALUES ()";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if(keys.next()){
                int id = keys.getInt(1);
                System.out.println("[DB] Match created with ID: " + id);
                return id;
            } else {
                throw new SQLException("Match creation failed, no ID generated.");
            }
        }
    }

    // For game created but where all player quit the lobby before starting
    public void deleteMatch(int gameId) throws SQLException {
        String sql = "DELETE FROM matches WHERE id = ?";

        try (Connection conn = connectionProvider.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, gameId);

            int rowAffected = ps.executeUpdate();
            if(rowAffected == 0){
                throw new SQLException("No match found with id: " + gameId);
            }
            System.out.println("[DB] Match " + gameId + " deleted.");
        }
    }

    public void saveMatch(int gameId, int playerCount,
                                 List<String> nicknames,
                                 List<Integer> scores) throws SQLException {

        String sqlMatch = "UPDATE matches SET played_at = CURRENT_TIMESTAMP, player_count = ? WHERE id = ?";
        String sqlUpsertPlayer = "INSERT IGNORE INTO players (nickname) VALUES (?)";
        String sqlGetPlayerId = "SELECT id FROM players WHERE nickname = ?";
        String sqlResult = "INSERT INTO results (match_id, player_id, score) VALUES (?, ?, ?)";

        try (Connection conn = connectionProvider.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psMatch = conn.prepareStatement(sqlMatch);
                 PreparedStatement psUpsert = conn.prepareStatement(sqlUpsertPlayer);
                 PreparedStatement psGetId = conn.prepareStatement(sqlGetPlayerId);
                 PreparedStatement psRes = conn.prepareStatement(sqlResult)) {

                try {
                    // Update Match
                    psMatch.setInt(1, playerCount);
                    psMatch.setInt(2, gameId);
                    psMatch.executeUpdate();

                    // Inserting Scores
                    for (int i = 0; i < nicknames.size(); i++) {
                        String nickname = nicknames.get(i);
                        psUpsert.setString(1, nickname);
                        psUpsert.executeUpdate();

                        psGetId.setString(1, nickname);
                        int playerId;
                        try (ResultSet rs = psGetId.executeQuery()) {
                            if (rs.next()) {
                                playerId = rs.getInt("id");
                            } else {
                                throw new SQLException("Player not found, upsert failed for: " + nickname);
                            }
                        }

                        int score = scores.get(i);
                        psRes.setInt(1, gameId);
                        psRes.setInt(2, playerId);
                        psRes.setInt(3, score);
                        psRes.executeUpdate();
                    }

                    conn.commit();
                    System.out.println("[DB] Match " + gameId + " saved.");

                } catch (SQLException e) {
                    conn.rollback();
                    System.err.println("[DB] Critical error, rollback executed.");
                    throw e;
                }
            }
        }
    }

    public List<DBRecord> getLeaderboard(int playerCount) throws SQLException {
        String sql = """
            SELECT
                p.nickname,
                SUM(r.score) AS total_score
            FROM results r
            JOIN matches m  ON r.match_id  = m.id
            JOIN players p  ON r.player_id = p.id
            WHERE m.player_count = ?
            GROUP BY p.id, p.nickname
            ORDER BY total_score DESC
            """;

        List<DBRecord> leaderboard = new ArrayList<>();
        try (Connection conn = connectionProvider.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, playerCount);
            ResultSet rs = ps.executeQuery();

            int currentRow = 1;
            int currentRank = 1;
            int previousScore = -1;
            boolean isFirstRow = true;

            while (rs.next()) {
                int score = rs.getInt("total_score");

                if (isFirstRow) {
                    isFirstRow = false;
                } else if (score < previousScore) {
                    currentRank = currentRow;
                }

                leaderboard.add(new DBRecord(
                        currentRank,
                        rs.getString("nickname"),
                        score
                ));

                previousScore = score;
                currentRow++;
            }
        }
        return leaderboard;
    }

    @Override
    public void setInitialCounter(int i) {}
}