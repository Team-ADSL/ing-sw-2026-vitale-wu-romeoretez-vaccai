package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Trigger;
import org.adsl.shared.network.requests.MoveRequest;
import org.adsl.shared.utils.Move;
import org.adsl.shared.enums.Row;
import org.adsl.shared.exceptions.ServerException;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.CardRow;

import java.util.*;

/**
 * State that handles the optional extra card draw granted by {@code ExtraMove}
 * buildings at end of round.
 * <p>
 * On entry it evaluates all players' end-of-round building effects. If exactly
 * one player has an {@code ExtraMove} bonus that player becomes the current
 * player and may pick one card from the upper row (or pass with an empty move
 * set). If no player has the bonus the state transitions immediately to
 * {@link EventsState}.
 * </p>
 */
public class ExtraMoveState extends ControllerState {
  public ExtraMoveState(Game game, GameController context) {
    super(game, context);
  }

  @Override
  public ControllerState onEntry() {
    setNextState(calcNextState());
    getGame().sendUpdateGame();
    return getNextState();
  }

  @Override
  public void visit(MoveRequest req, VirtualClient virtualClient) throws ServerException {
    Player reqPlayer = controlIfPlayerTurn(virtualClient);

    Set<Move> moves = req.getMoves();
    if (moves.size() > 1) {
      throw new ServerException("Invalid input, only 0 or 1 move allowed");
    }

    if (moves.isEmpty()) {
      getGame().setCurrentPlayer(null);
      getGame().setPhase(Phase.EVENTS_EXECUTION);
      setNextState(new EventsState(getGame(), getContext()));
      getGame().sendUpdateGame();
    } else {
      Move currentMove = moves.stream().findFirst().get();
      if (currentMove.row() != Row.UPPER) {
        throw new ServerException("Allowed only picking from TopRow");
      }

      CardRow selectedRow = getGame().getBoard().topRow();
      Card selectedCard = selectedRow.getCardAt(currentMove.rowIndex());
      if (selectedCard == null || !selectedCard.canBeDrawn(reqPlayer)) {
        throw new ServerException("Invalid picking: selected card cannot be picked");
      }

      execute(currentMove, reqPlayer);
    }
  }

  public void execute(Move move, Player p) {
    CardRow selectedRow = getGame().getBoard().topRow();
    Card selectedCard = selectedRow.pickCardAt(move.rowIndex());
    String pickName = selectedCard.getClass().getSimpleName();

    selectedCard.insert(p.getCards());
    p.changeFood(-selectedCard.getCost());
    selectedCard.activeEffect(Set.of(p), Trigger.DRAWING);
    p.setLastPick(selectedCard);

    Map<Player, int[]> before = snapshotFoodPp(Set.of(p));
    List<String> logsBuildingActivated = new ArrayList<>();

    for(Card b : p.getCards().get(CardType.BUILDINGS)){
      String title = b.getClass().getSimpleName();
      b.activeEffect(Set.of(p), Trigger.DRAWING);
      logsBuildingActivated.add(formatDeltas(title, Set.of(p), before));
    }

    String log = "[EXTRA MOVE] Player " + p.getName() + " picked 1 card: " + pickName + ".";
    List<String> logsBuildingRelevant = logsBuildingActivated.stream()
            .filter(l -> !l.contains("no change")).toList();
    if(!logsBuildingRelevant.isEmpty()) {
      log += "Activated following building: " + String.join(", ", logsBuildingRelevant) + ".";
    }
    System.out.println(log);

    getGame().setCurrentPlayer(null);
    getGame().setPhase(Phase.EVENTS_EXECUTION);
    setNextState(new EventsState(getGame(), getContext()));
    getGame().sendUpdateGame(log);
  }

  @Override
  public ControllerState calcNextState() {
    if (isToStop()) {
      return new RecoverState(getGame(), getContext());
    }
    getGame().getPlayers().forEach(
        p -> p.getCards().get(CardType.BUILDINGS).forEach(
            b -> b.activeEffect(Set.of(p), Trigger.END_ROUND)));

    Optional<Player> playerExtraMove = getGame().getPlayers().stream()
        .filter(p -> p.getBuildingBonus().isExtraMove())
        .findFirst();

    getGame().getPlayers().forEach(p -> p.getBuildingBonus().reset());

    if (playerExtraMove.isPresent()) {
      getGame().setCurrentPlayer(playerExtraMove.get());
      return this;
    } else {
      getGame().setCurrentPlayer(null);
      getGame().setPhase(Phase.EVENTS_EXECUTION);
      return new EventsState(getGame(), getContext());
    }
  }
}
