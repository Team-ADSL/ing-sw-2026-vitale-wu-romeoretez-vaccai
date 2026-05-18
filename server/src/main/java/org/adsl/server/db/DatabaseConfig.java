package org.adsl.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Static JDBC configuration for the MySQL leaderboard database.
 * <p>
 * The database password is read from the {@code DB_PASSWORD} environment
 * variable at class-load time. Connecting without that variable set causes
 * an immediate {@link SQLException}.
 * </p>
 */
public class DatabaseConfig {
    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    static final         String DB_NAME  = "game_leaderboard";

    static final         String USER     = "root";

    static final         String PASSWORD = System.getenv("DB_PASSWORD");

    public static final String URL_NO_DB =
            "jdbc:mysql://" + HOST + ":" + PORT +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    public static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null) {
            throw new SQLException("[SECURITY FATAL] Variabile d'ambiente 'DB_PASSWORD' non trovata. Impossibile connettersi al database.");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
