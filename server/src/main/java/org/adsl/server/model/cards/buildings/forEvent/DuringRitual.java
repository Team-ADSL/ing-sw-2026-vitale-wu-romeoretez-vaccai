package org.adsl.server.model.cards.buildings.forEvent;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingEffect;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Optional;
import java.util.Set;

/**
 * Building that modifies the {@code ShamanicRitual} event for its owner.
 * The specific modifier is controlled by {@link BuildingEffect}:
 * <ul>
 *   <li>{@code RITUAL_IMMUNITY} – the owner does not lose PP even if they have
 *       the fewest shaman stars.</li>
 *   <li>{@code RITUAL_STARS_BONUS} – grants +3 virtual shaman stars during the
 *       ritual.</li>
 *   <li>{@code RITUAL_DOUBLE_PP} – doubles the PP gained by the winner of the
 *       ritual (only if the owner is alone at the top).</li>
 * </ul>
 */
public class DuringRitual extends DuringEvent {
    private final BuildingEffect buildingEffect;

    /**
     * Creates a Shamanic Ritual-related building.
     *
     * @param id             unique card identifier
     * @param endGamePP      end-game PP awarded by this building
     * @param cost           food cost to acquire this building
     * @param era            era this card belongs to
     * @param numPlayers     number of players in the match
     * @param buildingEffect which ritual modifier this building applies
     *                       ({@code RITUAL_IMMUNITY}, {@code RITUAL_STARS_BONUS},
     *                       or {@code RITUAL_DOUBLE_PP})
     */
    public DuringRitual(String id, int endGamePP, int cost, int era, Integer numPlayers, BuildingEffect buildingEffect) {
        super(id, endGamePP, cost, era, numPlayers);
        this.buildingEffect = buildingEffect;
    }

    /**
     * Applies this building's {@link BuildingEffect} to the owner's
     * {@code BuildingBonus} when the trigger is {@link Trigger#SHAMANIC_RITUAL}:
     * grants ritual PP-loss immunity, +3 virtual shaman stars, or doubles PP
     * gained if the owner wins the ritual alone, depending on
     * {@code buildingEffect}. {@code players} is expected to contain exactly the
     * owning player (see {@link DuringEvent#activeEffect}); other triggers are
     * ignored.
     *
     * @param players singleton set containing the building's owner
     * @param t       the trigger that fired
     */
    @Override
    public void execute(Set<Player> players, Trigger t) {
        if (t != Trigger.SHAMANIC_RITUAL) {
            return;
        }
        Optional<Player> playerContainer = players.stream().findFirst();
        if (playerContainer.isEmpty()) {
            return; // ERRORE DA GESTIRE?
        }
        Player p = playerContainer.get();
        if (buildingEffect == BuildingEffect.RITUAL_IMMUNITY) {
            p.getBuildingBonus().setNoRitualLostPP(true);
        }
        if (buildingEffect == BuildingEffect.RITUAL_STARS_BONUS) {
            p.getBuildingBonus().setExtraStars(3);
        }
        if (buildingEffect == BuildingEffect.RITUAL_DOUBLE_PP) {
            p.getBuildingBonus().setShamanMulitiplierPP(2);
        }
    }

    @Override
    protected String getEffectsLabel() {
        return switch (buildingEffect) {
            case RITUAL_IMMUNITY    -> CardToken.RITUAL + ":" + CardToken.SHIELD;
            case RITUAL_STARS_BONUS -> CardToken.RITUAL + ":+3" + CardToken.SHAMAN_STAR;
            case RITUAL_DOUBLE_PP   -> CardToken.RITUAL + ":x2" + CardToken.PP;
            default                 -> "";
        };
    }
}
