
package org.adsl.server.model.cards.buildings;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.cards.characters.Builder;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

public abstract class Building extends Card {
    private final int endGamePP;
    private final int cost;

    public Building(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.endGamePP = endGamePP;
        this.cost = cost;
    }

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
        cards.get(CardType.BUILDINGS).add(this);
    }

    public int getCost() {
        return cost;
    }

    @Override
    protected String getCostLabel() {
        return CardToken.FOOD_COST + cost;
    }

    public int getEndGamePP() {
        return endGamePP;
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.BUILDING + " " + eraToRoman(getEra());
    }

    @Override
    public String narrationName() {
        return "Building";
    }
}
