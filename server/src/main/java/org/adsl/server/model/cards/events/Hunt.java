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
 * Event card: each player gains 1 food and {@code multiplierPP} PP per
 * {@code Hunter} card they own. If the player owns a {@code DuringHunt}
 * building they gain an additional 1 food and 1 PP per Hunter.
 */
public class Hunt extends Event {

    private final int multiplierPP;

    /**
     * Creates a Hunt event card.
     *
     * @param id           unique card identifier
     * @param multiplierPP PP gained per Hunter card owned
     * @param isFinal      {@code true} if this is the era-3/round-10 copy of the event
     * @param era          era this card belongs to
     * @param numPlayers   number of players in the match
     */
    public Hunt (String id, int multiplierPP, boolean isFinal, int era, Integer numPlayers) {
        super(id, isFinal, era, numPlayers);
        this.multiplierPP = multiplierPP;
    }

    /**
     * Adds this card to the {@link CardType#HUNT} set, if present in {@code cards}.
     *
     * @param cards the deck/board map of cards keyed by type
     */
    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.HUNT)) cards.get(CardType.HUNT).add(this);
    }

    /**
     * Resolves the Hunt event for each player when {@code t} is
     * {@link Trigger#EVENT_EXECUTION}. For each player, first activates any
     * {@code DuringHunt} building (via {@link #activateBuildings}), then grants
     * 1 food and {@code multiplierPP} PP per Hunter card owned. If the
     * {@code DuringHunt} bonus is active, the player gains an additional 1 food
     * and 1 PP per Hunter. The per-player {@code BuildingBonus} is reset at the
     * end of processing.
     *
     * @param players the players affected by the event
     * @param t       the trigger that fired; only {@link Trigger#EVENT_EXECUTION} is handled
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            for (Player p : players) {
                activateBuildings(p, Trigger.HUNT);
                BuildingBonus bonus = p.getBuildingBonus();
                int hunterCount = p.getCards().get(CardType.HUNTER).size();
                p.changePP(hunterCount * multiplierPP);
                p.changeFood(hunterCount);
                if(bonus.isHuntEventBonus()){
                    p.changePP(hunterCount);
                    p.changeFood(hunterCount);
                }
                bonus.reset();
            }
        }
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.HUNT + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return "1" + CardToken.FOOD + "+" + multiplierPP + CardToken.PP + " x " + CardToken.HUNTER;
    }

    @Override
    public String getEventTitle() {
        return "HUNT";
    }
}
