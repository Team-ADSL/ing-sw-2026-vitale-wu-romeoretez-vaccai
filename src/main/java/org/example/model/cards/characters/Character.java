package org.example.model.cards.characters;

import org.example.model.cards.Card;
import org.example.shared.enums.Trigger;
import org.example.model.Player;

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