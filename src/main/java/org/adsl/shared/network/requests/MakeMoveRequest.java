package org.adsl.shared.network.requests;

import org.adsl.server.exceptions.GameException;
import org.adsl.shared.utils.Move;

import java.util.Set;

public class MakeMoveRequest extends ClientRequest{
    private final Set<Move> moves;

    public MakeMoveRequest(Set<Move> moves) {
        this.moves = moves;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws GameException {
        visitor.visit(this, context);
    }

    public Set<Move> getMoves() {
        return moves;
    }
}
