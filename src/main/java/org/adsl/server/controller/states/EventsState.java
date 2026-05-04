package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;

import java.util.*;

public class EventsState extends ControllerState {

  public EventsState(Game game, GameController context) {
    super(game, context);
  }

  @Override
  public ControllerState onEntry() {
    Set<Player> players = getGame().getPlayers();
    ArrayList<Card> cards = getGame().getBoard().lowRow().getTribeCards();

    Map<CardType, Set<Card>> events = new EnumMap<>(CardType.class);
    events.put(CardType.HUNT, new HashSet<>());
    events.put(CardType.SHAMANIC_RITUAL, new HashSet<>());
    events.put(CardType.SUSTENANCE, new HashSet<>());
    events.put(CardType.CAVE_PAINTINGS, new HashSet<>());
    for (Card c : cards) {
      if (c != null) {
        c.insert(events);
      }
    }

    List<Card> nonSustenanceEvents = events.entrySet().stream()
        .filter(entry -> entry.getKey() != CardType.SUSTENANCE)
        .flatMap(entry -> entry.getValue().stream())
        .sorted(Comparator.comparingInt(Card::getEra))
        .toList();

    List<Card> sustenanceEvents = events.get(CardType.SUSTENANCE).stream()
        .sorted(Comparator.comparingInt(Card::getEra))
        .toList();

    for (Card c : nonSustenanceEvents) {
      c.activeEffect(players, Trigger.EVENT_EXECUTION);
    }
    for (Card c : sustenanceEvents) {
      c.activeEffect(players, Trigger.EVENT_EXECUTION);
    }

    setNextState(calcNextState());
    getGame().sendUpdateGame();
    return getNextState();
  }

  @Override
  public ControllerState calcNextState() {
    if (getGame().getRound() != 10) {
      getGame().setPhase(Phase.END_ROUND);
      return new EndRoundState(getGame(), getContext());
    } else {
      getGame().setPhase(Phase.END_GAME);
      return new EndGameState(getGame(), getContext());
    }
  }
}
