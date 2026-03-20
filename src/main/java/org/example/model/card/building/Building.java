package org.example.model.card.building;

import org.example.model.card.CardType;
import org.example.model.card.Card;
import org.example.model.game.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class Building extends Card {
    private int endGamePP;
    private int cost;

    public Building (int endGamePP, int cost, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.endGamePP = endGamePP;
        this.cost = cost;
    }

    @Override
    public boolean canBeDrawn(Player p) {
        if(p == null) {
            return false;
        }
        return p.getFood() >= cost;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {

    }
}
