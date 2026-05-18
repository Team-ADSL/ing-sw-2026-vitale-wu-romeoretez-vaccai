package org.adsl.server.model.cards.buildings;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Set;

/**
 * Building that grants +1 bonus food when the owner places their totem on the
 * order tile at the end of their action ({@link Trigger#END_TURN}).
 * The bonus is stored in {@code BuildingBonus.setBonusFoodTile()} and applied
 * in {@code ActionExecutionState}.
 */
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



    @Override
    protected String getEffectsLabel() {
        return CardToken.TOTEM + "+" + CardToken.FOOD;
    }
}
