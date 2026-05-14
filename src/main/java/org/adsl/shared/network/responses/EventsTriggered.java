package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.client.exceptions.InvalidResponseException;

/**
 * Sent by the server once per resolved end-of-round event, immediately after
 * the event's effect has been applied. The client paces these arrivals
 * (see AppCoordinator) to space them on screen.
 */
public class EventsTriggered extends ServerResponse {
    private final String eventTitle;

    @JsonCreator
    public EventsTriggered(@JsonProperty("eventTitle") String eventTitle) {
        this.eventTitle = eventTitle;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public String getEventTitle() {
        return eventTitle;
    }
}
