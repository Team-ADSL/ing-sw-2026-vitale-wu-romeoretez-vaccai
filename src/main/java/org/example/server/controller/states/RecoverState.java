package org.example.server.controller.states;

import org.example.shared.utils.Move;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.server.model.Game;
import org.example.server.model.Player;

import java.util.Set;

public class RecoverState extends State {
    public RecoverState(Game game) {
        super(game);
    }

    @Override
    public void checkMove(Set<Move> moves, Player p) throws InvalidMoveException {

    }

    @Override
    public void execute(Set<Move> moves, Player p) {

    }

    @Override
    public State nextState() {
        return null;
    }
}
