package org.example.shared.network.requests;

import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.network.RequestVisitor;

public class MakeMoveRequest extends ClientRequest{
    public MakeMoveRequest(int gameId, String username) {
        super(gameId, username);
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidMoveException {
        visitor.visit(this, context);
    }
}
