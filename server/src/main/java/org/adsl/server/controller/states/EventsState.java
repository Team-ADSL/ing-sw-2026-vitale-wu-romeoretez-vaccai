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

  /** {player → [food, pp]} snapshot taken right before an event resolves. */
  private static Map<Player, int[]> snapshotFoodPp(Set<Player> players) {
    Map<Player, int[]> out = new HashMap<>();
    for (Player p : players) {
      out.put(p, new int[]{ p.getFood(), p.getPp() });
    }
    return out;
  }

  /**
   * Builds a one-line summary of who gained/lost what during this event. Every
   * player is always listed so an unaffected player is visible too — players
   * with no food/PP change render as "no change". Used as the Game Log message
   * for the {@code EventsTriggered} response.
   */
  private static String formatDeltas(String title, Set<Player> players, Map<Player, int[]> before) {
    StringBuilder sb = new StringBuilder(title);
    sb.append(" — ");
    boolean first = true;
    for (Player p : players) {
      int[] prev = before.get(p);
      int dFood = p.getFood() - prev[0];
      int dPp   = p.getPp()   - prev[1];
      if (!first) sb.append(" | ");
      first = false;
      sb.append(p.getName()).append(": ");
      if (dFood == 0 && dPp == 0) {
        sb.append("no change");
      } else {
        boolean hasPp = false;
        if (dPp != 0) {
          sb.append(signed(dPp)).append(" PP");
          hasPp = true;
        }
        if (dFood != 0) {
          if (hasPp) sb.append(' ');
          sb.append(signed(dFood)).append(" food");
        }
      }
    }
    return sb.toString();
  }

  private static String signed(int n) {
    return (n > 0 ? "+" : "") + n;
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
