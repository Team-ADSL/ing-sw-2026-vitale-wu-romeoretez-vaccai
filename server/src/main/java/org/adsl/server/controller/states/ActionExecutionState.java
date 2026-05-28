package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.model.board.*;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.network.requests.MoveRequest;
import org.adsl.shared.utils.Move;
import org.adsl.shared.enums.Row;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Manual state in which the current player picks cards from the board rows.
 * <p>
 * On entry the state identifies the first player on the offer track who has not
 * yet acted and sets them as the current player. On each {@link MoveRequest} it
 * validates the draw counts against the chosen offer tile's allowed moves, picks
 * the cards, places the player's totem on the order tile, and applies any
 * end-of-turn building bonuses. If all players have acted the state transitions
 * to {@link ExtraMoveState}. Offer tiles that grant food are resolved automatically
 * without waiting for a client request.
 * </p>
 */
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
        setNextState(calcNextState());
      }
    }
    getGame().sendUpdateGame();
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
    Set<Move> moves = req.getMoves();

    OfferTile offerTile = getGame().getBoard().offerTrack().getTileAt(offerIndex);
    Map<Row, Integer> allowedMoves = offerTile.getMoves();
    int numLowDraw = (int) moves.stream().filter(m -> m.row() == Row.LOWER).count();
    int numUpDraw = (int) moves.stream().filter(m -> m.row() == Row.UPPER).count();

    int maxAllowedUpper = allowedMoves.getOrDefault(Row.UPPER, 0);
    int maxAllowedLower = allowedMoves.getOrDefault(Row.LOWER, 0);
    int pickableUpper = (int)getGame().getBoard().topRow().getTribeCards().stream()
            .filter(Objects::nonNull)
            .filter(c -> c.canBeDrawn(null))
            .count();
    int pickableLower = (int)getGame().getBoard().lowRow().getTribeCards().stream()
            .filter(Objects::nonNull)
            .filter(c -> c.canBeDrawn(null))
            .count();
    int minLowerAllowed = Math.min(pickableLower, maxAllowedLower);
    int minUpperAllowed = Math.min(pickableUpper, maxAllowedUpper);

    if (numUpDraw > maxAllowedUpper || numUpDraw < minUpperAllowed ||
            numLowDraw > maxAllowedLower || numLowDraw < minLowerAllowed ) {
      String upperRule = minUpperAllowed == maxAllowedUpper
              ? String.valueOf(maxAllowedUpper)
              : minUpperAllowed + "-" + maxAllowedUpper;
      String lowerRule = minLowerAllowed == maxAllowedLower
              ? String.valueOf(maxAllowedLower)
              : minLowerAllowed + "-" + maxAllowedLower;
      throw new ServerException(
          "Wrong card(s) picks: upper " + upperRule + ", lower " + lowerRule + ".");
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
    List<String> pickedNames = new ArrayList<>();
    List<String> logsBuildingActivated = new ArrayList<>();

    for (Move move : moves) {
      CardRow selectedRow;
      if (move.row() == Row.UPPER) {
        selectedRow = getGame().getBoard().topRow();
      } else {
        selectedRow = getGame().getBoard().lowRow();
      }

      Card selectedCard = selectedRow.pickCardAt(move.rowIndex());
      pickedNames.add(selectedCard.getClass().getSimpleName());
      selectedCard.insert(p.getCards());
      p.changeFood(-selectedCard.getCost());
      selectedCard.activeEffect(Set.of(p), Trigger.DRAWING);
      p.setLastPick(selectedCard);

      Map<Player, int[]> before = snapshotFoodPp(Set.of(p));
      for(Card b : p.getCards().get(CardType.BUILDINGS)){
        String title = b.toString();
        b.activeEffect(Set.of(p), Trigger.DRAWING);
        logsBuildingActivated.add(formatDeltas(title, Set.of(p), before));
      }
    }
    OfferTrack offerTrack = getGame().getBoard().offerTrack();
    offerTrack.removePlayer(p);
    placeTotem(p);

    String log = "[ACTION] Player " + p.getName() + " picked " + moves.size()
            + " card(s): " + String.join(", ", pickedNames) + ".";
    if(!logsBuildingActivated.isEmpty()) {
      log += "Activated following building: " + String.join(", ", logsBuildingActivated) + ".";
    }
    System.out.println(log);
    setNextState(calcNextState());
    getGame().sendUpdateGame(log);
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
