package org.adsl.shared.network.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.adsl.server.exceptions.ServerException;
import org.adsl.shared.utils.Move;

import java.util.Set;

public class MoveRequest extends ClientRequest{
    private final Set<Move> moves;

    @JsonCreator
    public MoveRequest(@JsonProperty("moves") Set<Move> moves) {
        this.moves = moves;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws ServerException {
        visitor.visit(this, context);
    }

    public Set<Move> getMoves() {
        return moves;
    }
}
