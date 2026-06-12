package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.enums.Totem;

/**
 * Sent when a client picks their totem colour, typically during the
 * game setup phase before play begins. The server validates that the
 * chosen totem is still available and assigns it to the client.
 */
public class TotemPickingRequest extends ClientRequest {
    private final Totem totem;

    /**
     * @param totem the totem colour the client wants to pick
     */
    @JsonCreator
    public TotemPickingRequest(@JsonProperty("totem") Totem totem) {
        this.totem = totem;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    /**
     * @return the totem colour the client wants to pick
     */
    public Totem getTotem() {
        return totem;
    }
}
