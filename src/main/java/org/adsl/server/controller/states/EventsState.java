package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.events.CavePaintings;
import org.adsl.server.model.cards.events.Hunt;
import org.adsl.server.model.cards.events.ShamanicRitual;
import org.adsl.server.model.cards.events.Sustenance;
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
    ArrayList<Card> cardsLower = getGame().getBoard().lowRow().getTribeCards();
    ArrayList<Card> cardsUpper = getGame().getBoard().topRow().getTribeCards();

    executeAndAnnounce(cardsLower);
    if (getGame().getRound() == 10) {
      executeAndAnnounce(cardsUpper);
    }
    setNextState(calcNextState());
    getGame().sendUpdateGame();
    return getNextState();
  }

  /**
   * Resolves every event in {@code cards} in the rules order (non-Sustenance by Era,
   * then Sustenance by Era). For each event the effect is applied first and the
   * announcement is sent right after, so the client receives one EventsTriggered
   * per event as soon as it has been resolved. The client paces the visual
   * delivery via the AppCoordinator dispatch pacer.
   */
  private void executeAndAnnounce(ArrayList<Card> cards) {
    Set<Player> players = getGame().getPlayers();

    Map<CardType, Set<Card>> events = new EnumMap<>(CardType.class);
    events.put(CardType.HUNT, new HashSet<>());
    events.put(CardType.SHAMANIC_RITUAL, new HashSet<>());
    events.put(CardType.SUSTENANCE, new HashSet<>());
    events.put(CardType.CAVE_PAINTINGS, new HashSet<>());
    for (Card c : cards) {
      if (c != null) c.insert(events);
    }

    List<Card> ordered = new ArrayList<>();
    events.entrySet().stream()
            .filter(e -> e.getKey() != CardType.SUSTENANCE)
            .flatMap(e -> e.getValue().stream())
            .sorted(Comparator.comparingInt(Card::getEra))
            .forEach(ordered::add);
    events.get(CardType.SUSTENANCE).stream()
            .sorted(Comparator.comparingInt(Card::getEra))
            .forEach(ordered::add);

    for (Card c : ordered) {
      c.activeEffect(players, Trigger.EVENT_EXECUTION);
      getGame().sendEventTriggered(formatTitle(c));
    }
  }

  private String formatTitle(Card c){
    String label;
    if (c instanceof Hunt) label = "HUNT";
    else if (c instanceof Sustenance) label = "SUSTENANCE";
    else if (c instanceof ShamanicRitual) label = "SHAMANIC RITUAL";
    else if (c instanceof CavePaintings) label = "CAVE PAINTINGS";
    else label = c.getId().toUpperCase();
    return label + " " + romanEra(c.getEra());
  }

  private String romanEra(int era){
    return switch (era) {
      case 1 -> "I";
      case 2 -> "II";
      case 3 -> "III";
      default -> String.valueOf(era);
    };
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
