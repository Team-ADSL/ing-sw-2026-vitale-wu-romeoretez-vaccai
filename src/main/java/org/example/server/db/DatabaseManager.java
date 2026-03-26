package org.example.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    public static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(
                DatabaseConfig.URL_NO_DB,
                DatabaseConfig.USER,
                DatabaseConfig.PASSWORD);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate(
                    "CREATE DATABASE IF NOT EXISTS " + DatabaseConfig.DB_NAME +
                            " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            );
            stmt.executeUpdate("USE " + DatabaseConfig.DB_NAME);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS players (
                    id       INT         AUTO_INCREMENT,
                    nickname VARCHAR(50) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE INDEX idx_nickname (nickname)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS matches (
                    id             INT     NOT NULL AUTO_INCREMENT,
                    played_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    player_count   TINYINT  NOT NULL,
                    PRIMARY KEY (id),
                    INDEX idx_player_count (player_count)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS results (
                    match_id    INT NOT NULL,
                    player_id   INT NOT NULL,
                    score       INT NOT NULL,
                    PRIMARY KEY (match_id, player_id),
                    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE,
                    FOREIGN KEY (match_id)  REFERENCES matches(id)  ON DELETE CASCADE,
                    INDEX idx_score (score DESC)
                )
            """);

            System.out.println("[DB] Database and tables ready.");

        } catch (SQLException e) {
            System.err.println("[DB] Initialization error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
