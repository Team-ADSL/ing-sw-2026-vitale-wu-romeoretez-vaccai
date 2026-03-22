package org.example.controller.state;

import org.example.controller.state.utils.Move;
import org.example.exception.InvalidMoveException;
import org.example.model.game.Game;
import org.example.model.game.Player;

import java.util.Set;

public class EndRoundState extends State {

    public EndRoundState(Game game) {
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
