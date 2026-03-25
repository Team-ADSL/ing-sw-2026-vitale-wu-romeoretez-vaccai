package org.example.model.cards.buildings;

import org.example.shared.enums.Trigger;
import org.example.model.Player;

import java.util.Optional;
import java.util.Set;

public class BonusTotem extends Building {

    public BonusTotem(int endGamePP, int cost, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, era, numPlayers);
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.END_TURN){
            players.stream().findFirst().ifPresent(p -> {
                p.getBuildingBonus().setBonusFoodTile(true);
            });
        }
    }
}
