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

    /**
     * Creates a BonusTotem building card.
     *
     * @param endGamePP  prestige points scored at end-game for owning this building
     * @param cost       food cost to acquire this building
     * @param id         unique card identifier
     * @param era        the era this card belongs to (1-3)
     * @param numPlayers minimum number of players required for this card to be in play, or {@code null} if always included
     */
    public BonusTotem(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
    }

    /**
     * When the owner ends their turn ({@link Trigger#END_TURN}), marks the bonus
     * food tile flag so the totem-placement bonus is applied by {@code ActionExecutionState}.
     *
     * @param players the affected players (expects a singleton containing the owner)
     * @param t       the trigger that fired
     */
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
