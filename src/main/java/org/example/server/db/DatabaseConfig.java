package org.example.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final String HOST     = "localhost";
    private static final String PORT     = "3306";
    static final         String DB_NAME  = "game_leaderboard";
    static final         String USER     = "root";
    static final         String PASSWORD = "Gianpaolo69!";

    public static final String URL_NO_DB =
            "jdbc:mysql://" + HOST + ":" + PORT +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    public static final String URL =
            "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
