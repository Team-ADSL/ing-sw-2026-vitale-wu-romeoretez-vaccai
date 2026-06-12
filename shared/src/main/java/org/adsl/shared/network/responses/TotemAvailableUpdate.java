package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;
import org.adsl.shared.enums.Totem;

import java.util.List;

/**
 * Response sent during the totem-picking phase after each player picks a
 * totem, broadcasting the updated list of totems still available for selection.
 */
public class TotemAvailableUpdate extends ServerResponse{
    private final List<Totem> totemAvailable;

    /**
     * Creates a response with the updated list of selectable totems.
     *
     * @param totems totems still available for selection
     */
    @JsonCreator
    public TotemAvailableUpdate(@JsonProperty("totemAvailable") List<Totem> totems){
        this.totemAvailable = totems;
    }

    /**
     * Dispatches this response to {@link ResponseVisitor#visit(TotemAvailableUpdate)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    /**
     * Returns the totems still available for selection.
     *
     * @return the list of available totems
     */
    public List<Totem> getTotemAvailable() {
        return totemAvailable;
    }
}
