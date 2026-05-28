package org.adsl.server.controller.states;

import org.adsl.server.controller.GameController;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Phase;
import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.events.Event;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Game;
import org.adsl.server.model.Player;

import java.util.*;

/**
 * Automatic state that resolves all event cards visible on the board.
 * <p>
 * Events in the lower row are always resolved. On the final round (10) the
 * upper-row events are resolved as well. Within each row events are processed
 * in rules order: non-Sustenance events by era first, then Sustenance events
 * by era. After resolution the state transitions to {@link EndRoundState}
 * (rounds 1–9) or {@link EndGameState} (round 10).
 * </p>
 */
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

    // Only Event subtypes are inserted into the HUNT/SHAMANIC_RITUAL/SUSTENANCE/
    // CAVE_PAINTINGS buckets (see each Event subclass's insert override), so the
    // cast below is safe by construction. Going through Event lets formatTitle
    // dispatch the label via polymorphism instead of branching on the subtype.
    List<Event> ordered = new ArrayList<>();
    events.entrySet().stream()
            .filter(e -> e.getKey() != CardType.SUSTENANCE)
            .flatMap(e -> e.getValue().stream())
            .map(c -> (Event) c)
            .sorted(Comparator.comparingInt(Event::getEra))
            .forEach(ordered::add);
    events.get(CardType.SUSTENANCE).stream()
            .map(c -> (Event) c)
            .sorted(Comparator.comparingInt(Event::getEra))
            .forEach(ordered::add);

    for (Event e : ordered) {
      Map<Player, int[]> before = snapshotFoodPp(players);
      e.activeEffect(players, Trigger.EVENT_EXECUTION);
      String title = formatTitle(e);
      String log = formatDeltas(title, players, before);
      getGame().sendEventTriggered(title, log);
    }
  }

  private String formatTitle(Event e){
    return e.getEventTitle() + " " + romanEra(e.getEra());
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
