
package org.adsl.server.model.cards.buildings;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.cards.characters.Builder;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

/**
 * Abstract base for all building cards.
 * <p>
 * Buildings are purchased by spending food (reduced by {@link Builder} discounts)
 * and grant end-game PP plus a subclass-specific in-game effect activated by
 * a {@code Trigger}. Buildings cannot be drawn by a {@code null} player and
 * always check food + builder discount against their cost.
 * </p>
 */
public abstract class Building extends Card {
    private final int endGamePP;
    private final int cost;

    /**
     * Creates a Building card.
     *
     * @param id         unique card identifier
     * @param endGamePP  prestige points scored at end-game for owning this building
     * @param cost       food cost to acquire this building
     * @param era        the era this card belongs to (1-3)
     * @param numPlayers minimum number of players required for this card to be in play, or {@code null} if always included
     */
    public Building(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.endGamePP = endGamePP;
        this.cost = cost;
    }

    /**
     * Returns {@code true} if {@code p} can afford this building, i.e. if their
     * current food plus the sum of all {@link Builder} discounts in their hand
     * is at least the building's {@code cost}. A {@code null} player can never
     * draw a building.
     *
     * @param p the player attempting to draw, or {@code null}
     * @return {@code true} if the player can afford this building
     */
    @Override
    public boolean canBeDrawn(Player p) {
        if (p == null) {
            return false;
        }
        int discount = p.getCards().get(CardType.BUILDER).stream()
                .map(c -> (Builder)c)
                .mapToInt(Builder::getDiscount)
                .sum();
        return p.getFood() + discount >= cost;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.BUILDINGS)) cards.get(CardType.BUILDINGS).add(this);
    }

    @Override
    public int getCost(){ return cost; }

    public int getEndGamePP() {
        return endGamePP;
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.BUILDING + eraToRoman(getEra())
                + "  " + cost + CardToken.FOOD
                + endGamePP + CardToken.PP;
    }
}
