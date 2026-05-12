package org.adsl.shared.network.responses;

import org.adsl.client.exceptions.InvalidResponseException;
import org.adsl.shared.enums.Totem;

import java.util.List;

public class TotemAvailableUpdate extends ServerResponse{
    private final List<Totem> totemAvailable;

    public TotemAvailableUpdate(List<Totem> totems, String message){
        this.totemAvailable = totems;
        super(message);
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public List<Totem> getTotemAvailable() {
        return totemAvailable;
    }
}
