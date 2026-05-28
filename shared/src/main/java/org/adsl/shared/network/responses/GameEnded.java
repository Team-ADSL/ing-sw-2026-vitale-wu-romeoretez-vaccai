package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;
import org.adsl.shared.model.DBRecord;
import org.adsl.shared.model.MatchResult;

import java.util.List;

/**
 * Response broadcast to all players when a game ends.
 * <p>
 * {@code results} holds the per-player final scores ({@link org.adsl.shared.model.MatchResult}
 * — nickname, prestige points, food) used to render the end-game screen.
 * {@code records} holds the all-time leaderboard fetched from the database after
 * saving the match; it is empty when the server runs without a DBMS.
 * Both lists are {@code null} if the game ended before it started
 * (all lobby players left without the game being started).
 * </p>
 */
public class GameEnded extends ServerResponse {
    private final List<DBRecord> records;
    private final List<MatchResult> results;

    @JsonCreator
    public GameEnded(@JsonProperty("records") List<DBRecord> records,
                     @JsonProperty("results") List<MatchResult> results) {
        this.results = results;
        this.records = records;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<DBRecord> getRecords() {
        return records;
    }
    public List<MatchResult> getResults() {
        return results;
    }
}
