package org.example.model.card.character;

import org.example.model.card.Card;
import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.game.Player;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Builder extends Character {

    private int discount;
    private int pp;

    public Builder(int discount, int pp, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.discount = discount;
        this.pp = pp;
    }

    public int getDiscount() {
        return discount;
    }

    public void setDiscount(int discount) {
        this.discount = discount;
    }

    public int getPp() {
        return pp;
    }

    public void setPp(int pp) {
        this.pp = pp;
    }

    @Override
    public boolean canBeDrawn(Player p) {
        return false;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {

    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {

    }
}
