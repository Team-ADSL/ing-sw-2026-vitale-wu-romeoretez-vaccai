package org.adsl.server.model.cards.characters;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;

import java.util.Set;

public abstract class Character extends Card {

    public Character (String id, int era, Integer numPlayers){
        super(id, era, numPlayers);
    }

    @Override
    public boolean canBeDrawn(Player p) {
        return true;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {}
}