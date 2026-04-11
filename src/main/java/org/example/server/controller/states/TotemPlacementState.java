package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.network.VirtualClient;
import org.example.shared.enums.Phase;
import org.example.shared.enums.Row;
import org.example.shared.network.requests.MakeMoveRequest;
import org.example.shared.utils.Move;
import org.example.server.exceptions.InvalidRequestException;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderTile;

import java.util.Set;


public class TotemPlacementState extends ControllerState {

    public TotemPlacementState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        Player reqPlayer = controlIfPlayerTurn(virtualClient);

        Set<Move> moves = req.getMoves();
        if(moves.size() != 1){
            throw new InvalidRequestException("Invalid input, only 1 move allowed");
        }

        Move currentMove = moves.stream().findFirst().get();
        if(!currentMove.row().equals(Row.OFFER)){
            throw new InvalidRequestException("Invalid input, you need to choose a tile from the offer track.");
        }
        execute(currentMove, reqPlayer);
    }

    public void execute(Move move, Player p) {
        OrderTile orderTile = getGame().getBoard().orderTile();
        orderTile.removePlayer(p);

        OfferTrack offerTrack = getGame().getBoard().offerTrack();
        offerTrack.placeInOfferTile(p, move.rowIndex());

        setNextState(calcNextState());
        getGame().sendUpdateGame();
    }

    @Override
    public ControllerState calcNextState() {
        if(isToStop()){
            return new RecoverState(getGame(), getContext());
        }
        OrderTile orderTile = getGame().getBoard().orderTile();
        int orderIndex = 0;
        while(orderTile.getPlayerAt(orderIndex).isEmpty()){
            orderIndex++;
            if(orderIndex == orderTile.size()){
                break;
            }
        }
        if(orderIndex != orderTile.size()){
            getGame().setCurrentPlayer(orderTile.getPlayerAt(orderIndex).orElse(null));
            return this;
        } else {
            getGame().setCurrentPlayer(null);
            getGame().setPhase(Phase.ACTION_EXECUTION);
            return new ActionExecutionState(getGame(), getContext());
        }
    }
}
