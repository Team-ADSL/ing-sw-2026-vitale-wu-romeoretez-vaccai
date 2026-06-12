package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Map;
import java.util.Set;

/**
 * Character card. If {@code extraFood} is {@code true}, the owner immediately
 * gains food equal to the total number of Hunters they own each time a Hunter
 * is drawn ({@link Trigger#DRAWING}). Hunters also multiply food and PP gained
 * during the {@code Hunt} event.
 */
public class Hunter extends Character {

    private final boolean extraFood;

    /**
     * Creates a Hunter character card.
     *
     * @param id         unique card identifier
     * @param extraFood  if {@code true}, drawing this card grants the owner immediate
     *                   food equal to their total Hunter count (see {@link #activeEffect})
     * @param era        the era this card belongs to (1-3)
     * @param numPlayers minimum number of players required for this card to be in play, or {@code null} if always included
     */
    public Hunter (String id, boolean extraFood, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.extraFood = extraFood;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.HUNTER)) cards.get(CardType.HUNTER).add(this);
    }

    /**
     * If {@code extraFood} is set and the card was just drawn ({@link Trigger#DRAWING}),
     * grants the owner food equal to their current Hunter count plus one. Must be
     * called after this card has already been inserted into the player's hand so the
     * count includes itself.
     *
     * @param players the affected players (expects a singleton containing the owner)
     * @param t       the trigger that fired
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.DRAWING && extraFood){
            Player p = players.stream()
                    .findFirst()
                    .orElse(null);
            if(p != null){
                int numHunter = p.getCards().get(CardType.HUNTER).size();
                p.changeFood(numHunter + 1);
            }
        }
    }

    @Override
    protected String getTypeLabel() {
        return CardToken.HUNTER + " " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return extraFood ? CardToken.MEAT : "";
    }
}
