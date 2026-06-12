package org.adsl.server.model.cards.buildings.forEvent;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Optional;
import java.util.Set;

/**
 * Building that grants a bonus during the {@code Hunt} event: the owner gains
 * +1 food and +1 PP per {@code Hunter} card they own (on top of the standard
 * Hunt reward). The bonus is stored in {@code BuildingBonus.setHuntEventBonus()}
 * and applied in {@code Hunt.activeEffect()}.
 */
public class DuringHunt extends DuringEvent {
    /**
     * Creates a Hunt-related building.
     *
     * @param id         unique card identifier
     * @param endGamePP  end-game PP awarded by this building
     * @param cost       food cost to acquire this building
     * @param era        era this card belongs to
     * @param numPlayers number of players in the match
     */
    public DuringHunt(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
    }

    /**
     * Sets the owner's {@code huntEventBonus} flag when the trigger is
     * {@link Trigger#HUNT}, so {@code Hunt.activeEffect()} grants the extra
     * +1 food/+1 PP per Hunter. {@code players} is expected to contain exactly
     * the owning player (see {@link DuringEvent#activeEffect}); other triggers
     * are ignored.
     *
     * @param players singleton set containing the building's owner
     * @param t       the trigger that fired
     */
    @Override
    public void execute(Set<Player> players, Trigger t) {
        if (t != Trigger.HUNT) {
            return;
        }
        Optional<Player> playerContainer = players.stream().findFirst();
        if (playerContainer.isEmpty()) {
            return; // ERRORE DA GESTIRE?
        }
        Player p = playerContainer.get();
        p.getBuildingBonus().setHuntEventBonus(true);
    }



    @Override
    protected String getEffectsLabel() {
        return CardToken.HUNT + ": +1" + CardToken.FOOD + "+1" + CardToken.PP + "x" + CardToken.HUNTER;
    }
}
