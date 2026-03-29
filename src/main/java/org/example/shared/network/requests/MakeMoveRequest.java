package org.example.shared.network.requests;

import org.example.shared.exceptions.InvalidRequestException;
import org.example.shared.network.RequestVisitor;
import org.example.shared.utils.Move;

import java.util.Set;

public class MakeMoveRequest extends ClientRequest{
    private final Set<Move> moves;

    public MakeMoveRequest(int gameId, String username, Set<Move> moves) {
        super(gameId, username);
        this.moves = moves;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }

    public Set<Move> getMoves() {
        return moves;
    }
}
