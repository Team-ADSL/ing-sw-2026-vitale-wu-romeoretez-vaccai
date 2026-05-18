package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.InvalidResponseException;
import org.adsl.shared.model.GameDTO;


/**
 * Response sent after every game-state change. Carries a full {@code GameDTO}
 * snapshot; the client replaces its entire model from this object.
 */
public class GameUpdate extends ServerResponse{
    private final GameDTO game;

    @JsonCreator
    public GameUpdate(@JsonProperty("game") GameDTO game) {
        this.game = game;
    }

    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    public GameDTO getGame() {
        return game;
    }
}
