package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.server.network.VirtualClient;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.shared.enums.Trigger;
import org.adsl.shared.network.requests.MoveRequest;
import org.adsl.shared.utils.Move;
import org.adsl.shared.enums.Row;
import org.adsl.server.exceptions.ServerException;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;
import org.adsl.server.model.board.CardRow;

import java.util.Optional;
import java.util.Set;

public class ExtraMoveState extends ControllerState {
  public ExtraMoveState(Game game, GameController context) {
    super(game, context);
  }

  @Override
  public ControllerState onEntry() {
    setNextState(calcNextState());
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
      reqPlayer.getBuildingBonus().setExtraMove(false);
      setNextState(calcNextState());
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
    selectedCard.insert(p.getCards());
    p.getBuildingBonus().setExtraMove(false);
    setNextState(calcNextState());
    getGame().sendUpdateGame();
  }

  @Override
  public ControllerState calcNextState() {
    if (isToStop()) {
      return new RecoverState(getGame(), getContext());
    }
    // TODO: need to add a controll for already executed action
    getGame().getPlayers().forEach(
        p -> p.getCards().get(CardType.BUILDINGS).forEach(
            b -> b.activeEffect(Set.of(p), Trigger.END_ROUND)));

    Optional<Player> playerExtraMove = getGame().getPlayers().stream()
        .filter(p -> p.getBuildingBonus().isExtraMove())
        .findFirst();

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
