package org.example.server.controller.states;

import org.example.server.network.VirtualClient;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Phase;
import org.example.shared.enums.Trigger;
import org.example.shared.network.requests.MakeMoveRequest;
import org.example.shared.utils.Move;
import org.example.shared.enums.Row;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.server.model.cards.Card;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTrack;

import java.util.Optional;
import java.util.Set;

public class ExtraMoveState extends ControllerState {
    public ExtraMoveState(Game game) {
        super(game);
    }

    @Override
    public ControllerState onEntry(){
        getGame().setPhase(Phase.EXTRA_MOVE);
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
        if(currentMove.getRow() != Row.UPPER){
            throw new InvalidRequestException("Allowed only picking from TopRow");
        }

        CardRow selectedRow = getGame().getBoard().getTopRow();
        Card selectedCard = selectedRow.pickCardAt(currentMove.getRowIndex());
        if(!selectedCard.canBeDrawn(reqPlayer)){
            throw new InvalidRequestException("Invalid picking: selected card cannot be picked");
        }

        execute(currentMove, reqPlayer);
    }

    public void execute(Move move, Player p) {
        CardRow selectedRow = getGame().getBoard().getTopRow();
        Card selectedCard = selectedRow.pickCardAt(move.getRowIndex());
        selectedCard.insert(p.getCards());
    }

    @Override
    public ControllerState nextState() {
        getGame().getPlayers().forEach(
                p -> p.getCards().get(CardType.BUILDINGS).forEach(
                        b -> b.activeEffect(Set.of(p), Trigger.END_ROUND)));

        Optional<Player> playerExtraMove = getGame().getPlayers().stream()
                .filter(p -> p.getBuildingBonus().isExtraMove())
                .findFirst();

        if(playerExtraMove.isPresent()){
            getGame().setCurrentPlayer(playerExtraMove);
            return this;
        } else {
            getGame().setCurrentPlayer(Optional.empty());
            return new EventsState(getGame());
        }
    }
}
