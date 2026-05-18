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

    public Hunter (String id, boolean extraFood, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.extraFood = extraFood;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.HUNTER)) cards.get(CardType.HUNTER).add(this);
    }

    // NOTE: to call after inserting in player's deck
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.DRAWING && extraFood){
            Player p = players.stream()
                    .findFirst()
                    .orElse(null);
            if(p != null){
                int numHunter = p.getCards().get(CardType.HUNTER).size();
                p.changeFood(numHunter);
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
