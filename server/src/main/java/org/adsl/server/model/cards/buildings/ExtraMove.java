package org.adsl.server.model.cards.buildings;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.characters.Character;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Set;

/**
 * Building that grants the owner an extra card draw from the upper row at the
 * end of the round ({@code Trigger.END_ROUND}), handled by {@code ExtraMoveState}.
 * The bonus is stored in {@code BuildingBonus.setExtraMove()}.
 */
public class ExtraMove extends Building {

    /**
     * Creates an ExtraMove building card.
     *
     * @param id           unique card identifier
     * @param endGamePP    prestige points scored at end-game for owning this building
     * @param cost         food cost to acquire this building
     * @param trigger      unused, reserved for future configuration of the activation trigger
     * @param characterUse unused, reserved for future configuration of character-based conditions
     * @param era          the era this card belongs to (1-3)
     * @param numPlayers   minimum number of players required for this card to be in play, or {@code null} if always included
     */
    public ExtraMove(String id, int endGamePP, int cost, Trigger trigger,
                     Set<Character> characterUse, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
    }

    /**
     * At the end of the round ({@link Trigger#END_ROUND}), marks the extra-move
     * flag so the owner gets an additional card draw from the upper row,
     * handled by {@code ExtraMoveState}.
     *
     * @param players the affected players (expects a singleton containing the owner)
     * @param t       the trigger that fired
     */
    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.END_ROUND){
            players.stream().findFirst().ifPresent(p -> {
                p.getBuildingBonus().setExtraMove(true);
            });
        }
    }



    @Override
    protected String getEffectsLabel() {
        return "+1 " + CardToken.EXTRA_MOVE;
    }
}
