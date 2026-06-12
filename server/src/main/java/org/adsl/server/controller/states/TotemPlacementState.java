package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Row;
import org.adsl.shared.network.requests.MoveRequest;
import org.adsl.shared.utils.Move;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.OfferTile;
import org.adsl.server.model.board.OfferTrack;
import org.adsl.server.model.board.OrderTile;

import java.util.Set;


/**
 * Manual state in which players, in turn-order, place their totem on an offer
 * tile to choose their action for the round.
 * <p>
 * Players are served in the order they appear on the {@link OrderTile}. Each
 * must submit a {@link MoveRequest} targeting a free {@link OfferTile} slot.
 * Once all players have placed their totems the state transitions to
 * {@link ActionExecutionState}.
 * </p>
 */
public class TotemPlacementState extends ControllerState {

    public TotemPlacementState(Game game, GameController context) {
        super(game, context);
    }

    /**
     * If no current player is set (start of round or recovery), selects the
     * first player still present on the {@link OrderTile} and notifies clients
     * of the updated game state.
     *
     * @return {@code this}, since this is a manual state waiting for the
     *         current player's {@link MoveRequest}
     */
    @Override
    public ControllerState onEntry() throws ServerException {
        if (getGame().getCurrentPlayer().isEmpty()) {
            OrderTile orderTile = getGame().getBoard().orderTile();
            int orderIndex = 0;
            while (orderIndex < orderTile.size() && orderTile.getPlayerAt(orderIndex).isEmpty()) {
                orderIndex++;
            }
            if (orderIndex < orderTile.size()) {
                getGame().setCurrentPlayer(orderTile.getPlayerAt(orderIndex).orElse(null));
            }
        }
        getGame().sendUpdateGame();
        return this;
    }

    /**
     * Validates and applies the current player's totem placement on a free
     * {@link OfferTile}.
     *
     * @param req           the move request, must contain exactly one move
     *                       targeting a free tile in the {@code OFFER} row
     * @param virtualClient the client sending the request
     * @throws ServerException if it is not the client's turn, more or fewer
     *                          than one move is submitted, the move does not
     *                          target the offer row, the index is out of range,
     *                          or the selected tile is already occupied
     */
    @Override
    public void visit(MoveRequest req, VirtualClient virtualClient) throws ServerException {
        Player reqPlayer = controlIfPlayerTurn(virtualClient);

        Set<Move> moves = req.getMoves();
        if(moves.size() != 1){
            throw new ServerException("Invalid input, only 1 move allowed");
        }

        Move currentMove = moves.stream().findFirst().get();
        if(!currentMove.row().equals(Row.OFFER)){
            throw new ServerException("Invalid input, you need to choose a tile from the offer track.");
        }

        OfferTrack offerTrack = getGame().getBoard().offerTrack();
        int idx = currentMove.rowIndex();
        if (idx < 0 || idx >= offerTrack.size()) {
            throw new ServerException("Invalid offer tile index: " + idx);
        }
        OfferTile selectedTile = offerTrack.getTileAt(idx);
        if (selectedTile.getPlayer().isPresent()) {
            throw new ServerException("That tile is already occupied!");
        }

        execute(currentMove, reqPlayer);
    }

    /**
     * Removes {@code p} from the order tile, places their totem on the chosen
     * offer tile, advances the state machine, and broadcasts the result.
     *
     * @param move the validated move, targeting the chosen offer tile
     * @param p    the player making the placement
     */
    public void execute(Move move, Player p) {
        OrderTile orderTile = getGame().getBoard().orderTile();
        orderTile.removePlayer(p);

        OfferTrack offerTrack = getGame().getBoard().offerTrack();
        offerTrack.placeInOfferTile(p, move.rowIndex());

        String log = "[TOTEM PLACEMENT] Player " + p.getName() + " placed totem at " + (move.rowIndex() + 1) + "° offer tile";
        System.out.println(log);
        setNextState(calcNextState());
        getGame().sendUpdateGame(log);
    }

    /**
     * @return a {@link RecoverState} if a player disconnected, {@code this}
     *         with the next player on the order tile set as current if any
     *         remain, or a new {@link ActionExecutionState} once every player
     *         has placed their totem
     */
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
