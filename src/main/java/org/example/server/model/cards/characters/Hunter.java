package org.example.server.model.cards.characters;

import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.Player;

import java.util.Map;
import java.util.Set;

public class Hunter extends Character {

    private final boolean extraFood;

    public Hunter (String id, boolean extraFood, int era, Integer numPlayers) {
        super(id, era, numPlayers);
        this.extraFood = extraFood;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        cards.get(CardType.HUNTER).add(this);
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
}
