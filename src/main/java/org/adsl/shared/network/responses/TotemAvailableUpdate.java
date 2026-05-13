package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.client.exceptions.InvalidResponseException;
import org.adsl.shared.enums.Totem;

import java.util.List;

public class TotemAvailableUpdate extends ServerResponse{
    private final List<Totem> totemAvailable;

    @JsonCreator
    public TotemAvailableUpdate(@JsonProperty("totemAvailable") List<Totem> totems){
        this.totemAvailable = totems;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<Totem> getTotemAvailable() {
        return totemAvailable;
    }
}
