package org.adsl.server.persistence;

import org.adsl.shared.model.DBRecord;

import java.sql.SQLException;
import java.util.List;

/**
 * Data-access interface for match history persistence.
 * <p>
 * The production implementation is {@link SqlGameDAO}. A match row is created
 * when the game starts and finalised (with timestamp, player count, and scores)
 * when the game ends.
 * </p>
 */
public interface GameDAO {

    /**
     * Inserts a new match row and returns the generated match ID.
     *
     * @return the auto-generated match ID
     * @throws SQLException if the insert fails
     */
    int createMatch() throws SQLException;

    /**
     * Deletes the match row with the given ID. Called when all players leave the
     * lobby before the game ever starts.
     *
     * @param gameId the match ID to delete
     * @throws SQLException if the delete fails or no row is found
     */
    void deleteMatch(int gameId) throws SQLException;

    /**
     * Finalises a match by recording its timestamp, player count, and per-player
     * scores. Upserts player rows as needed.
     *
     * @param gameId      the match ID created by {@link #createMatch()}
     * @param playerCount number of players who participated
     * @param nicknames   player nicknames in the same order as {@code scores}
     * @param scores      final prestige-point totals
     * @throws SQLException if the transaction fails (automatically rolled back)
     */
    void saveMatch(int gameId, int playerCount,
                          List<String> nicknames,
                          List<Integer> scores) throws SQLException;

    /**
     * Returns the all-time leaderboard for matches of the given player count,
     * ranked by cumulative score descending.
     *
     * @param playerCount filter for matches with this exact number of players
     * @return ordered list of {@link DBRecord}s with dense rank
     * @throws SQLException if the query fails
     */
    List<DBRecord> getLeaderboard(int playerCount) throws SQLException;

    /**
     * @param i max index of the games recovered
     */
    void setInitialCounter(int i);
}
