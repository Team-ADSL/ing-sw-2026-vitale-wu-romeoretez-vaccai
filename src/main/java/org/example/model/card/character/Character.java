package org.example.model.card.character;

import org.example.model.card.Card;
import org.example.model.card.Trigger;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public abstract class Character extends Card {

    public Character (int era, Optional<Integer> numPlayers){
        super(era, numPlayers);
    }

    @Override
    public boolean canBeDrawn(Player p) {
        return true;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {}
}