package org.example.server.controller.states;

import org.example.server.controller.GameController;
import org.example.server.network.VirtualClient;
import org.example.shared.enums.Phase;
import org.example.shared.network.requests.MakeMoveRequest;
import org.example.shared.utils.Move;
import org.example.shared.enums.Row;
import org.example.shared.exceptions.InvalidRequestException;
import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.Game;
import org.example.server.model.Player;
import org.example.server.model.board.CardRow;
import org.example.server.model.board.OfferTile;
import org.example.server.model.board.OfferTrack;
import org.example.server.model.board.OrderCell;

import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class ActionExecutionState extends ControllerState {

    public ActionExecutionState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public ControllerState onEntry(){
        getGame().setPhase(Phase.ACTION_EXECUTION);
        getGame().sendUpdateGame();
        return nextState();
    }

    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws InvalidRequestException {
        Player reqPlayer = controlIfPlayerTurn(req, virtualClient);

        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        int offerIndex = 0;
        while(offerTrack.getTileAt(offerIndex).getPlayer().isEmpty() && offerIndex < offerTrack.size()){
            offerIndex++;
        }
        int remainingMoves = offerTrack.getTileAt(offerIndex).getNumMoves();
        Set<Move> moves = req.getMoves();
        if(moves.size() != remainingMoves){
            throw new InvalidRequestException(
                    "Number of cards mismatch. Requires " + moves.size()
                            + ", Allowed: " + remainingMoves);
        }

        OfferTile offerTile = getGame().getBoard().getOfferTrack().getTileAt(offerIndex);
        Map<Row, Integer> allowedMoves = offerTile.getMoves();
        int numLowDraw = (int) moves.stream().filter(m -> m.getRow() == Row.LOWER).count();
        int numUpDraw = (int) moves.stream().filter(m -> m.getRow() == Row.UPPER).count();
        if(numUpDraw != allowedMoves.get(Row.UPPER) || numLowDraw != allowedMoves.get(Row.LOWER)){
            throw new InvalidRequestException(
                    "Wrong moves: you can do "
                            + allowedMoves.get(Row.UPPER) + " draws from the up row and "
                            + allowedMoves.get(Row.LOWER) + " draws from the low row");
        }

        for(Move move : moves){
            CardRow selectedRow = null;
            if(move.getRow() == Row.UPPER) {
                selectedRow = getGame().getBoard().getTopRow();
            } else {
                selectedRow = getGame().getBoard().getLowRow();
            }
            Card selectedCard = selectedRow.pickCardAt(move.getRowIndex());
            if(!selectedCard.canBeDrawn(reqPlayer)){
                throw new InvalidRequestException("Invalid picking: some selected cards cannot be picked");
            }
        }

        execute(moves, reqPlayer);
    }

    private void execute(Set<Move> moves, Player p) {
        for(Move move : moves){
            CardRow selectedRow = null;
            if(move.getRow() == Row.UPPER) {
                selectedRow = getGame().getBoard().getTopRow();
            } else {
                selectedRow = getGame().getBoard().getLowRow();
            }
            Card selectedCard = selectedRow.pickCardAt(move.getRowIndex());
            selectedCard.insert(p.getCards());
        }
        placeTotem(p);
        getGame().sendUpdateGame();
    }

    private void placeTotem(Player p){
        int i = getGame().getBoard().getOrderTile().placePlayerAtNext(p);

        // Eventual bonus for totem placement in order tile
        p.getCards().get(CardType.BUILDINGS).forEach(b -> b.activeEffect(Set.of(p),Trigger.END_TURN));

        // Set new point/food after player's move
        OrderCell orderCell = getGame().getBoard().getOrderTile().getCellAt(i);
        if(orderCell.getBonus() >= 0){
            p.changeFood(orderCell.getBonus());
            if(p.getBuildingBonus().isBonusFoodTile()){
                p.changeFood(1);
            }
        } else if (orderCell.isMalus()){
            if(p.getFood() != 0){
                p.changeFood(-1);
            } else{
                p.changePP(-2);
            }
        }
    }

    @Override
    public ControllerState nextState() {
        if(isToStop()){
            return new RecoverState(getGame(), getContext());
        }
        OfferTrack offerTrack = getGame().getBoard().getOfferTrack();
        int offerIndex = 0;
        while(offerTrack.getTileAt(offerIndex).getPlayer().isEmpty() && offerIndex < offerTrack.size()){
            offerIndex++;
        }

        Optional<Player> currPlayer = offerTrack.getTileAt(offerIndex).getPlayer();
        if(currPlayer.isPresent()){
            if(offerTrack.getTileAt(offerIndex).isGivesFood()){
                currPlayer.get().changeFood(3);
                placeTotem(currPlayer.get());
                return nextState();
            } else {
                getGame().setCurrentPlayer(currPlayer.get());
                return this;
            }
        } else {
            getGame().setCurrentPlayer(null);
            return new ExtraMoveState(getGame(), getContext());
        }
    }
}