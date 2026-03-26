package org.example.server.model.cards.buildings;

import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.server.model.cards.characters.Builder;
import org.example.server.model.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class Building extends Card {
    private final int endGamePP;
    private final int cost;

    public Building(int endGamePP, int cost, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
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

    public int getEndGamePP() {
        return endGamePP;
    }
}
