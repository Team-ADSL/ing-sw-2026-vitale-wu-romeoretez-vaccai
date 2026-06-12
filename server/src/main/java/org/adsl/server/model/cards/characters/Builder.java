package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

/**
 * Character card that reduces the food cost of buildings by {@code discount} and
 * contributes {@code pp} prestige points at end-game (resolved in
 * {@code EndGameState}). Multiple builders stack their discounts.
 */
public class Builder extends Character {

    private final int discount;
    private final int pp;

    /**
     * Creates a Builder character card.
     *
     * @param id         unique card identifier
     * @param discount   food discount applied to building costs while this card is in hand
     * @param pp         prestige points contributed at end-game
     * @param era        the era this card belongs to (1-3)
     * @param numPlayers minimum number of players required for this card to be in play, or {@code null} if always included
     */
    public Builder(String id, int discount, int pp, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.discount = discount;
        this.pp = pp;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.BUILDER))cards.get(CardType.BUILDER).add(this);
    }

    public int getDiscount() {
        return discount;
    }

    public int getPP() {
        return pp;
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.BUILDER + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return "-" + discount + CardToken.FOOD + " " + pp + CardToken.PP;
    }
}
