package org.example.shared.network.requests;

import org.example.server.exceptions.InvalidRequestException;
import org.example.shared.utils.Move;

import java.util.Set;

public class MakeMoveRequest extends ClientRequest{
    private final int gameId;
    private final Set<Move> moves;

    public MakeMoveRequest(int gameId, Set<Move> moves) {
        this.gameId = gameId;
        this.moves = moves;
    }

    @Override
    public <T> void accept(RequestVisitor<T> visitor, T context) throws InvalidRequestException {
        visitor.visit(this, context);
    }

    public Set<Move> getMoves() {
        return moves;
    }
    public int getGameId() {
        return gameId;
    }
}
