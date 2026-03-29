package org.example.server.controller.states;

import org.example.server.network.VirtualClient;
import org.example.shared.enums.Phase;
import org.example.shared.network.requests.MakeMoveRequest;
import org.example.shared.utils.Move;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;

import java.util.Optional;
import java.util.Set;


public class TotemPlacementState extends ControllerState {

    public TotemPlacementState(Game game) {
        super(game);
    }

    @Override
    public ControllerState onEntry(){
        getGame().setPhase(Phase.TOTEM_PLACEMENT);
        return nextState();
    }

    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        Player reqPlayer = controlIfPlayerTurn(req);

        Set<Move> moves = req.getMoves();
        if(moves.size() != 1){
            throw new InvalidRequestException("Invalid input, only 1 move allowed");
        }

        Move currentMove = moves.stream().findFirst().get();
        execute(currentMove, reqPlayer);
    }

    public void execute(Move move, Player p) {
        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        offerTrack.placeInOfferTile(p, move.getRowIndex());
    }

    @Override
    public ControllerState nextState() {
        OrderTile orderTile = getGame().getBoard().getOrderTile();
        int orderIndex = 0;
        while(orderTile.getPlayerAt(orderIndex).isEmpty() && orderIndex < orderTile.size()){
            orderIndex++;
        }
        if(orderIndex != getGame().getPlayers().size() - 1){
            getGame().setCurrentPlayer(orderTile.getPlayerAt(orderIndex));
            return this;
        } else {
            getGame().setCurrentPlayer(Optional.empty());
            return new ActionExecutionState(getGame());
        }
    }
}
