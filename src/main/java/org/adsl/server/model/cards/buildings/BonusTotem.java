package org.adsl.server.model.cards.buildings;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;

import java.util.Set;

public class BonusTotem extends Building {

    public BonusTotem(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
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
