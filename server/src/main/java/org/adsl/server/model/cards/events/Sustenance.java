package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

/**
 * Event card: each player must pay 1 food per character card they own, reduced
 * by 3 food per {@code Gatherer} and by any {@code DuringSustenance} discount.
 * For each character that cannot be fed the player loses {@code lostPP} PP instead.
 */
public class Sustenance extends Event {

    private final int lostPP;

    /**
     * Creates a Sustenance event card.
     *
     * @param id         unique card identifier
     * @param lostPP     PP lost per character card that cannot be fed
     * @param isFinal    {@code true} if this is the era-3/round-10 copy of the event
     * @param era        era this card belongs to
     * @param numPlayers number of players in the match
     */
    public Sustenance(String id, int lostPP, boolean isFinal, int era, Integer numPlayers) {
        super(id, isFinal, era, numPlayers);
        this.lostPP = lostPP;
    }

    /**
     * Adds this card to the {@link CardType#SUSTENANCE} set, if present in
     * {@code cards}.
     *
     * @param cards the deck/board map of cards keyed by type
     */
    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.SUSTENANCE)) cards.get(CardType.SUSTENANCE).add(this);
    }

    /**
     * Resolves the Sustenance event for each player when {@code t} is
     * {@link Trigger#EVENT_EXECUTION}. First activates any {@code DuringSustenance}
     * building (via {@link #activateBuildings}), then computes the food cost as
     * the total number of character cards (all cards except buildings) minus a
     * discount (3 food per {@code Gatherer} card plus
     * {@link BuildingBonus#getSustenanceDiscount()}). The player pays as much of
     * that cost as their food allows; for each unit of cost left unpaid, the
     * player loses {@code lostPP} PP instead. The per-player
     * {@code BuildingBonus} is reset at the end of processing.
     *
     * @param players the players affected by the event
     * @param t       the trigger that fired; only {@link Trigger#EVENT_EXECUTION} is handled
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            for (Player p : players) {
                activateBuildings(p, Trigger.SUSTENANCE);
                BuildingBonus bonus = p.getBuildingBonus();
                int totalCharacters = p.getCards().entrySet().stream()
                        .filter(e -> e.getKey() != CardType.BUILDINGS)
                        .mapToInt(e -> e.getValue().size()).sum();
                int discount = p.getCards().get(CardType.GATHERER).size() * 3 + bonus.getSustenanceDiscount();
                int foodToPay = Math.max(0, totalCharacters - discount);
                int foodAvailable = p.getFood();
                int actualFoodPaid = Math.min(foodAvailable, foodToPay);
                p.changeFood(-actualFoodPaid);
                int unfedCharacters = foodToPay - actualFoodPaid;
                p.changePP(-(unfedCharacters * lostPP));
                bonus.reset();
            }
        }
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.SUSTENANCE + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return "-1" + CardToken.FOOD + "/" + lostPP + CardToken.PP + " x" + CardToken.CHARACTER;
    }

    @Override
    public String getEventTitle() {
        return "SUSTENANCE";
    }
}
