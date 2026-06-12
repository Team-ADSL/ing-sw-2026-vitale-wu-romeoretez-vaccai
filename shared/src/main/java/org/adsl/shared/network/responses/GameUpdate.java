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

    /**
     * Creates a response carrying a full game-state snapshot.
     *
     * @param game the current game state
     */
    @JsonCreator
    public GameUpdate(@JsonProperty("game") GameDTO game) {
        this.game = game;
    }

    /**
     * Dispatches this response to {@link ResponseVisitor#visit(GameUpdate)}.
     *
     * @param visitor the visitor that will handle this response
     * @throws InvalidResponseException if the visitor cannot process this response
     */
    @Override
    public void accept(ResponseVisitor visitor) throws InvalidResponseException {
        visitor.visit(this);
    }

    /**
     * Returns the full game-state snapshot carried by this response.
     *
     * @return the current game state
     */
    public GameDTO getGame() {
        return game;
    }
}
