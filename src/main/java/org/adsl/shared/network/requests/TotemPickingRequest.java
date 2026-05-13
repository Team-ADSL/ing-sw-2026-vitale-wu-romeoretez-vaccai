package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.enums.Totem;

public class TotemPickingRequest extends ClientRequest {
    private final Totem totem;

    @JsonCreator
    public TotemPickingRequest(@JsonProperty("totem") Totem totem) {
        this.totem = totem;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    public Totem getTotem() {
        return totem;
    }
}
