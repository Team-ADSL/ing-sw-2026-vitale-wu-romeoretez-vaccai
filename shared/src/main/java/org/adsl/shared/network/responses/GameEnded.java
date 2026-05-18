package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;
import org.adsl.shared.model.MatchResult;

import java.util.List;

/**
 * Response broadcast to all players when a game ends. Carries the final
 * leaderboard; if {@code results} is {@code null} the game ended before it
 * started (all lobby players left).
 */
public class GameEnded extends ServerResponse {
    private final List<MatchResult> results;

    @JsonCreator
    public GameEnded(@JsonProperty("results") List<MatchResult> results) {
        this.results = results;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<MatchResult> getResults() {
        return results;
    }
}
