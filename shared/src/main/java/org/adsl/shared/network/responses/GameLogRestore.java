package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;

import java.util.List;

/**
 * Sent to a single client when it (re)joins a game in progress, carrying the
 * full game-log transcript accumulated server-side. Lets a reconnecting client
 * (typically a fresh process whose in-memory log is empty) repopulate its game
 * log so the transcript survives a disconnect/reconnect. The client replaces
 * its local log with this history rather than appending, so a same-process
 * reconnect does not duplicate entries.
 */
public class GameLogRestore extends ServerResponse {
    private final List<String> history;

    @JsonCreator
    public GameLogRestore(@JsonProperty("history") List<String> history) {
        this.history = history;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<String> getHistory() {
        return history;
    }
}
