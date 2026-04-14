package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.network.requests.MakeMoveRequest;
import org.adsl.shared.utils.Move;
import org.adsl.shared.enums.Row;
import org.adsl.server.exceptions.GameException;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.CardRow;
import org.adsl.server.model.board.OfferTile;
import org.adsl.server.model.board.OfferTrack;
import org.adsl.server.model.board.OrderCell;

import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class ActionExecutionState extends ControllerState {

    public ActionExecutionState(Game game, GameController context) {
        super(game, context);
    }

    @Override
    public void visit(MakeMoveRequest req, VirtualClient virtualClient) throws GameException {
        Player reqPlayer = controlIfPlayerTurn(virtualClient);

        OfferTrack offerTrack = getGame().getBoard().offerTrack();
        int offerIndex = 0;
        while(offerTrack.getTileAt(offerIndex).getPlayer().isEmpty() && offerIndex < offerTrack.size()){
            offerIndex++;
        }
        int remainingMoves = offerTrack.getTileAt(offerIndex).getNumMoves();
        Set<Move> moves = req.getMoves();
        if(moves.size() != remainingMoves){
            throw new GameException(
                    "Number of cards mismatch. Requires " + moves.size()
                            + ", Allowed: " + remainingMoves);
        }

        OfferTile offerTile = getGame().getBoard().offerTrack().getTileAt(offerIndex);
        Map<Row, Integer> allowedMoves = offerTile.getMoves();
        int numLowDraw = (int) moves.stream().filter(m -> m.row() == Row.LOWER).count();
        int numUpDraw = (int) moves.stream().filter(m -> m.row() == Row.UPPER).count();
        if(numUpDraw != allowedMoves.get(Row.UPPER) || numLowDraw != allowedMoves.get(Row.LOWER)){
            throw new GameException(
                    "Wrong moves: you can do "
                            + allowedMoves.get(Row.UPPER) + " draws from the up row and "
                            + allowedMoves.get(Row.LOWER) + " draws from the low row");
        }

        for(Move move : moves){
            CardRow selectedRow;
            if(move.row() == Row.UPPER) {
                selectedRow = getGame().getBoard().topRow();
            } else {
                selectedRow = getGame().getBoard().lowRow();
            }
            Card selectedCard = selectedRow.pickCardAt(move.rowIndex());
            if(!selectedCard.canBeDrawn(reqPlayer)){
                throw new GameException("Invalid picking: " +
                        "card at " + move.row().toString() + " row and index " +
                        move.rowIndex() + " cannot be picked");
            }
        }

        execute(moves, reqPlayer);
    }

    private void execute(Set<Move> moves, Player p) {
        for(Move move : moves){
            CardRow selectedRow;
            if(move.row() == Row.UPPER) {
                selectedRow = getGame().getBoard().topRow();
            } else {
                selectedRow = getGame().getBoard().lowRow();
            }
            Card selectedCard = selectedRow.pickCardAt(move.rowIndex());
            selectedCard.insert(p.getCards());
        }
        OfferTrack offerTrack = getGame().getBoard().offerTrack();
        offerTrack.removePlayer(p);
        placeTotem(p);
        setNextState(calcNextState());
        getGame().sendUpdateGame();
    }

    private void placeTotem(Player p){
        int i = getGame().getBoard().orderTile().placePlayerAtNext(p);

        // Eventual bonus for totem placement in order tile
        p.getCards().get(CardType.BUILDINGS).forEach(b -> b.activeEffect(Set.of(p),Trigger.END_TURN));

        // Set new point/food after player's move
        OrderCell orderCell = getGame().getBoard().orderTile().getCellAt(i);
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
    public ControllerState calcNextState() {
        if(isToStop()){
            return new RecoverState(getGame(), getContext());
        }
        OfferTrack offerTrack = getGame().getBoard().offerTrack();
        int offerIndex = 0;
        while(offerTrack.getTileAt(offerIndex).getPlayer().isEmpty() && offerIndex < offerTrack.size()){
            offerIndex++;
        }

        Optional<Player> currPlayer = offerTrack.getTileAt(offerIndex).getPlayer();
        if(currPlayer.isPresent()){
            if(offerTrack.getTileAt(offerIndex).isGivesFood()){
                // First update model (previous action) and execute the automation after
                getGame().sendUpdateGame();
                // Automatic action can be performed
                getGame().setCurrentPlayer(currPlayer.get());
                currPlayer.get().changeFood(3);
                offerTrack.removePlayer(currPlayer.get());
                placeTotem(currPlayer.get());
                return calcNextState();
            } else {
                getGame().setCurrentPlayer(currPlayer.get());
                return this;
            }
        } else {
            getGame().setCurrentPlayer(null);
            getGame().setPhase(Phase.EXTRA_MOVE);
            return new ExtraMoveState(getGame(), getContext());
        }
    }
}