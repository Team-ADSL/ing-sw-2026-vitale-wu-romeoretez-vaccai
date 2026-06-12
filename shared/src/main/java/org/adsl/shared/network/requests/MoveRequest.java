package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.shared.utils.Move;

import java.util.Set;

/**
 * Sent when a client performs one or more moves during their turn in an
 * active game. The server validates and applies the moves to the game state,
 * then broadcasts the resulting update to all players.
 */
public class MoveRequest extends ClientRequest{
    private final Set<Move> moves;

    /**
     * @param moves the set of moves the client is performing
     */
    @JsonCreator
    public MoveRequest(@JsonProperty("moves") Set<Move> moves) {
        this.moves = moves;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    /**
     * @return the set of moves the client is performing
     */
    public Set<Move> getMoves() {
        return moves;
    }
}
