package org.example.server.controller.states;

import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.shared.exceptions.InvalidMoveException;
import org.example.shared.utils.Move;

import java.util.Set;

public class LobbyState extends State{
    public LobbyState(Game game) {
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
