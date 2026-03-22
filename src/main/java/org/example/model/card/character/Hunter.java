package org.example.model.card.character;

import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.game.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Hunter extends Character {

    private final boolean extraFood;

    public Hunter (boolean extraFood, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.extraFood = extraFood;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        cards.get(CardType.HUNTER).add(this);
    }

    // NOTE: to call after inserting in player's deck
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.DRAWING){
            Player p = players.stream()
                    .findFirst()
                    .orElse(null);
            if(p != null){
                int numHunter = p.getCards().get(CardType.HUNTER).size();
                p.changeFood(numHunter);
            }
        }
    }

    public boolean isExtraFood() {
        return extraFood;
    }
}
