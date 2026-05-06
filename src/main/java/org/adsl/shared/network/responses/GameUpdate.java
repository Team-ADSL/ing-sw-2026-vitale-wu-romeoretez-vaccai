package org.adsl.shared.network.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.client.exceptions.InvalidResponseException;
import org.adsl.shared.model.GameDTO;


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
