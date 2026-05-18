package org.adsl.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initialises the MySQL database schema on server startup.
 * Creates the {@code game_leaderboard} database and the {@code players},
 * {@code matches}, and {@code results} tables if they do not already exist.
 */
public class DatabaseManager {

    /**
     * Creates the database and all required tables using {@code CREATE IF NOT EXISTS}
     * statements. Throws {@link RuntimeException} wrapping the underlying
     * {@link SQLException} if the schema cannot be set up.
     *
     * @throws RuntimeException if {@code DB_PASSWORD} is not set or the DDL fails
     */
    public static void initDatabase() {
        if (DatabaseConfig.PASSWORD == null) {
            throw new RuntimeException(new SQLException(
                    "[SECURITY FATAL] Variabile d'ambiente 'DB_PASSWORD' non trovata. Impossibile connettersi al database."));
        }
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
                    id INT NOT NULL AUTO_INCREMENT,
                    nickname VARCHAR(50) NOT NULL,
                    PRIMARY KEY (id),
                    UNIQUE INDEX idx_nickname (nickname)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS matches (
                    id INT NOT NULL AUTO_INCREMENT ,
                    played_at DATETIME,
                    player_count TINYINT,
                    PRIMARY KEY (id),
                    INDEX idx_player_count (player_count)
                )
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS results (
                    match_id INT NOT NULL,
                    player_id INT NOT NULL,
                    score INT NOT NULL,
                    PRIMARY KEY (match_id, player_id),
                    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE,
                    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
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
