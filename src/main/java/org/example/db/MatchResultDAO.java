package org.example.db;
import org.example.model.MatchResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatchResultDAO {

    /**
     * Saves a completed match. For each player:
     * - creates a new entry if the nickname is new
     * - updates best_score, matches_played and last_played if it already exists
     */
    public static void saveMatch(int playerCount,
                                 List<String> nicknames,
                                 List<Integer> scores) throws SQLException {

        String sqlMatch =
                "INSERT INTO matches (player_count) VALUES (?)";

        String sqlUpsertPlayer = """
            INSERT INTO players (nickname, best_score, matches_played, last_played)
            VALUES (?, ?, 1, NOW())
            ON DUPLICATE KEY UPDATE
                best_score     = GREATEST(best_score, VALUES(best_score)),
                matches_played = matches_played + 1,
                last_played    = NOW()
            """;

        String sqlGetPlayerId =
                "SELECT id FROM players WHERE nickname = ?";

        String sqlResult =
                "INSERT INTO results (player_id, match_id, score) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Insert the match, get generated id
                int matchId;
                try (PreparedStatement ps = conn.prepareStatement(
                        sqlMatch, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, playerCount);
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    keys.next();
                    matchId = keys.getInt(1);
                }

                // 2. For each player: upsert → get id → insert result
                for (int i = 0; i < nicknames.size(); i++) {
                    String nickname = nicknames.get(i);
                    int    score    = scores.get(i);

                    try (PreparedStatement ps = conn.prepareStatement(sqlUpsertPlayer)) {
                        ps.setString(1, nickname);
                        ps.setInt(2, score);
                        ps.executeUpdate();
                    }

                    int playerId;
                    try (PreparedStatement ps = conn.prepareStatement(sqlGetPlayerId)) {
                        ps.setString(1, nickname);
                        ResultSet rs = ps.executeQuery();
                        rs.next();
                        playerId = rs.getInt("id");
                    }

                    try (PreparedStatement ps = conn.prepareStatement(sqlResult)) {
                        ps.setInt(1, playerId);
                        ps.setInt(2, matchId);
                        ps.setInt(3, score);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
                System.out.println("[DB] Match saved successfully.");

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Returns the leaderboard rank of a given score
     * among matches with the same player count.
     */
    public static int getRank(int score, int playerCount) throws SQLException {
        String sql = """
            SELECT COUNT(*) + 1 AS rank
            FROM results r
            JOIN matches m ON r.match_id = m.id
            WHERE m.player_count = ? AND r.score > ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerCount);
            ps.setInt(2, score);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt("rank") : -1;
        }
    }

    /**
     * Returns the full leaderboard for a given player count.
     */
    public static List<MatchResult> getLeaderboard(int playerCount) throws SQLException {
        String sql = """
            SELECT
                RANK() OVER (ORDER BY r.score DESC) AS rank,
                p.nickname,
                r.score,
                m.played_at
            FROM results r
            JOIN matches m  ON r.match_id  = m.id
            JOIN players p  ON r.player_id = p.id
            WHERE m.player_count = ?
            ORDER BY r.score DESC
            LIMIT 100
            """;
        List<MatchResult> leaderboard = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playerCount);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                leaderboard.add(new MatchResult(
                        rs.getInt("rank"),
                        rs.getString("nickname"),
                        rs.getInt("score"),
                        rs.getTimestamp("played_at").toLocalDateTime()
                ));
            }
        }
        return leaderboard;
    }

    /**
     * Prints global stats for a given nickname.
     */
    public static void printPlayerStats(String nickname) throws SQLException {
        String sql = """
            SELECT nickname, best_score, matches_played, last_played
            FROM players
            WHERE nickname = ?
            """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nickname);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                System.out.println("Player       : " + rs.getString("nickname"));
                System.out.println("Best score   : " + rs.getInt("best_score"));
                System.out.println("Matches played: " + rs.getInt("matches_played"));
                System.out.println("Last played  : " + rs.getTimestamp("last_played"));
            } else {
                System.out.println("Player not found.");
            }
        }
    }
}