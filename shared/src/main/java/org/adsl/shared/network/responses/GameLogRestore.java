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

    /**
     * Creates a response carrying the full server-side game-log transcript.
     *
     * @param history ordered list of log-line strings making up the transcript
     */
    @JsonCreator
    public GameLogRestore(@JsonProperty("history") List<String> history) {
        this.history = history;
    }

    /**
     * Dispatches this response to {@link ResponseVisitor#visit(GameLogRestore)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    /**
     * Returns the full game-log transcript to restore on the client.
     *
     * @return the ordered list of log-line strings
     */
    public List<String> getHistory() {
        return history;
    }
}
