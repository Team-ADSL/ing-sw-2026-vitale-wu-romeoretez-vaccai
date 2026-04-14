package org.adsl.shared.network.responses;

import org.adsl.client.exceptions.InvalidResponseException;
import org.adsl.shared.model.GameDTO;


public class GameUpdate extends ServerResponse{
    private final GameDTO game;

    public GameUpdate(GameDTO game) {
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
