package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

/**
 * Character card that reduces the food cost per character during the
 * {@code Sustenance} event. Each Gatherer provides a fixed {@code discount}
 * (in food) applied by {@code Sustenance.activeEffect()}.
 */
public class Gatherer extends Character {

    private final int discount;

    /**
     * Creates a Gatherer character card.
     *
     * @param id         unique card identifier
     * @param discount   per-character food discount applied during the {@code Sustenance} event
     * @param era        the era this card belongs to (1-3)
     * @param numPlayers minimum number of players required for this card to be in play, or {@code null} if always included
     */
    public Gatherer (String id, int discount, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.discount = discount;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.GATHERER)) cards.get(CardType.GATHERER).add(this);
    }

    public int getDiscount() {
        return discount;
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.GATHERER + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return CardToken.FOOD_COST + discount;
    }
}
