package org.example.shared.network.responses;

import org.example.client.exceptions.InvalidResponseException;
import org.example.shared.model.GameDTO;


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
