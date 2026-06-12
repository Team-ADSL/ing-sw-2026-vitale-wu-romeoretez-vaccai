package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;

/**
 * Sent by the server once per resolved end-of-round event, immediately after
 * the event's effect has been applied. Carries both the short {@code eventTitle}
 * shown as the on-screen overlay and the longer {@code logMessage} that the
 * client appends to the game log (per-player food/PP deltas). The client paces
 * these arrivals (see AppCoordinator) to space them on screen.
 */
public class EventsTriggered extends ServerResponse {
    private final String eventTitle;
    private final String logMessage;

    /**
     * Creates a response for a single resolved end-of-round event.
     *
     * @param eventTitle short title shown as the on-screen event overlay
     * @param logMessage longer description appended to the client's game log
     */
    @JsonCreator
    public EventsTriggered(@JsonProperty("eventTitle") String eventTitle,
                           @JsonProperty("logMessage") String logMessage) {
        this.eventTitle = eventTitle;
        this.logMessage = logMessage;
    }

    /**
     * Dispatches this response to {@link ResponseVisitor#visit(EventsTriggered)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    /**
     * Marks this response as an event overlay, so the client holds it on screen
     * long enough for players to read the per-player food/PP deltas.
     */
    @Override
    public boolean isEventOverlay() {
        return true;
    }

    /**
     * Returns the short title shown as the on-screen event overlay.
     *
     * @return the event title
     */
    public String getEventTitle() {
        return eventTitle;
    }

    /**
     * Returns the longer description appended to the client's game log.
     *
     * @return the log message
     */
    public String getLogMessage() {
        return logMessage;
    }
}
