package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.client.exceptions.InvalidResponseException;

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

    @JsonCreator
    public EventsTriggered(@JsonProperty("eventTitle") String eventTitle,
                           @JsonProperty("logMessage") String logMessage) {
        this.eventTitle = eventTitle;
        this.logMessage = logMessage;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getLogMessage() {
        return logMessage;
    }
}
