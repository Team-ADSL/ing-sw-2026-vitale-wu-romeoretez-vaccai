package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.board.*;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.network.requests.MoveRequest;
import org.adsl.shared.utils.Move;
import org.adsl.shared.enums.Row;
import org.adsl.server.exceptions.ServerException;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ActionExecutionState extends ControllerState {

  public ActionExecutionState(Game game, GameController context) {
    super(game, context);
  }

  @Override
  public ControllerState onEntry() throws ServerException {
    if (getGame().getCurrentPlayer().isEmpty()) {
      OfferTrack offerTrack = getGame().getBoard().offerTrack();
      int orderIndex = 0;
      while (orderIndex < offerTrack.size() && offerTrack.getTileAt(orderIndex).getPlayer().isEmpty()) {
        orderIndex++;
      }
      if (orderIndex < offerTrack.size()) {
        getGame().setCurrentPlayer(offerTrack.getTileAt(orderIndex).getPlayer().orElse(null));
        getGame().sendUpdateGame();
      }
    }
    return this;
  }

  @Override
  public void visit(MoveRequest req, VirtualClient virtualClient) throws ServerException {
    Player reqPlayer = controlIfPlayerTurn(virtualClient);

    OfferTrack offerTrack = getGame().getBoard().offerTrack();
    int offerIndex = 0;
    while (offerIndex < offerTrack.size() && offerTrack.getTileAt(offerIndex).getPlayer().isEmpty()) {
      offerIndex++;
    }
    int remainingMoves = offerTrack.getTileAt(offerIndex).getNumMoves();
    Set<Move> moves = req.getMoves();
    if (moves.size() != remainingMoves) {
      throw new ServerException(
          "Number of cards mismatch. Required: " + remainingMoves
              + ", provided: " + moves.size());
    }

    OfferTile offerTile = getGame().getBoard().offerTrack().getTileAt(offerIndex);
    Map<Row, Integer> allowedMoves = offerTile.getMoves();
    int numLowDraw = (int) moves.stream().filter(m -> m.row() == Row.LOWER).count();
    int numUpDraw = (int) moves.stream().filter(m -> m.row() == Row.UPPER).count();
    int maxAllowedUpper = allowedMoves.getOrDefault(Row.UPPER, 0);
    int maxAllowedLower = allowedMoves.getOrDefault(Row.LOWER, 0);
    int minUpperAllowed = (int)getGame().getBoard().topRow().getTribeCards().stream()
            .filter(c -> c.canBeDrawn(null))
            .count();
    int minLowerAllowed = (int)getGame().getBoard().lowRow().getTribeCards().stream()
            .filter(c -> c.canBeDrawn(null))
            .count();
    if (numUpDraw > maxAllowedUpper || numUpDraw < minUpperAllowed ||
            numLowDraw > maxAllowedLower || numLowDraw < minLowerAllowed ) {
      throw new ServerException(
          "Wrong moves: you can draw max "
              + maxAllowedUpper + " card(s), min " + minUpperAllowed + "from the top row and "
              + maxAllowedLower + " card(s), min " + minLowerAllowed + "from the top row.");
    }

    for (Move move : moves) {
      CardRow selectedRow;
      if (move.row() == Row.UPPER) {
        selectedRow = getGame().getBoard().topRow();
      } else {
        selectedRow = getGame().getBoard().lowRow();
      }
      Card selectedCard = selectedRow.getCardAt(move.rowIndex());
      if (selectedCard == null || !selectedCard.canBeDrawn(reqPlayer)) {
        throw new ServerException("Invalid picking: " +
            "card at " + move.row().toString() + " row and index " +
            move.rowIndex() + " cannot be picked");
      }
    }

    execute(moves, reqPlayer);
  }

  private void execute(Set<Move> moves, Player p) {
    for (Move move : moves) {
      CardRow selectedRow;
      if (move.row() == Row.UPPER) {
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
    System.out.println("[ACTION] Player " + p.getName() + " picked " + moves.size() + " card(s).");
    setNextState(calcNextState());
    getGame().sendUpdateGame();
  }

  private void placeTotem(Player p) {
    int i = getGame().getBoard().orderTile().placePlayerAtNext(p);

    // Eventual bonus for totem placement in order tile
    p.getCards().get(CardType.BUILDINGS).forEach(b -> b.activeEffect(Set.of(p), Trigger.END_TURN));

    // Set new point/food after player's move
    OrderCell orderCell = getGame().getBoard().orderTile().getCellAt(i);
    if (orderCell.getBonus() >= 0) {
      p.changeFood(orderCell.getBonus());
      if (p.getBuildingBonus().isBonusFoodTile()) {
        p.changeFood(1);
      }
    } else if (orderCell.isMalus()) {
      if (p.getFood() != 0) {
        p.changeFood(-1);
      } else {
        p.changePP(-2);
      }
    }

    getGame().getPlayers().forEach(player -> player.getBuildingBonus().reset());
  }

  @Override
  public ControllerState calcNextState() {
    if (isToStop()) {
      return new RecoverState(getGame(), getContext());
    }
    OfferTrack offerTrack = getGame().getBoard().offerTrack();
    int offerIndex = 0;
    while (offerIndex < offerTrack.size() && offerTrack.getTileAt(offerIndex).getPlayer().isEmpty()) {
      offerIndex++;
    }

    Optional<Player> currPlayer = (offerIndex < offerTrack.size())
        ? offerTrack.getTileAt(offerIndex).getPlayer()
        : Optional.empty();
    if (currPlayer.isPresent()) {
      if (offerTrack.getTileAt(offerIndex).isGivesFood()) {
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
