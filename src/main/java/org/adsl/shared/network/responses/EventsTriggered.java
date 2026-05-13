package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.client.exceptions.InvalidResponseException;

import java.util.Collections;
import java.util.List;

public class EventsTriggered extends ServerResponse {
    private final List<String> eventTitles;
    private final long durationMs;

    @JsonCreator
    public EventsTriggered(@JsonProperty("eventTitles") List<String> eventTitles,
                           @JsonProperty("durationMs") long durationMs) {
        this.eventTitles = eventTitles != null ? eventTitles : Collections.emptyList();
        this.durationMs = durationMs;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<String> getEventTitles() {
        return eventTitles;
    }

    public long getDurationMs() {
        return durationMs;
    }
}