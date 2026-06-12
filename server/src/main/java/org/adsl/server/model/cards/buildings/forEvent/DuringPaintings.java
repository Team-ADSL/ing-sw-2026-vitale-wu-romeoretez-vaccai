package org.adsl.server.model.cards.buildings.forEvent;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Optional;
import java.util.Set;

/**
 * Building that grants +1 food per {@code Artist} card owned by the player
 * during the {@code CavePaintings} event. The bonus is stored in
 * {@code BuildingBonus.setArtistFood()} and applied in
 * {@code CavePaintings.activeEffect()}.
 */
public class DuringPaintings extends DuringEvent {
    /**
     * Creates a Cave Paintings-related building.
     *
     * @param id         unique card identifier
     * @param endGamePP  end-game PP awarded by this building
     * @param cost       food cost to acquire this building
     * @param era        era this card belongs to
     * @param numPlayers number of players in the match
     */
    public DuringPaintings(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
    }

    /**
     * Sets the owner's {@code artistFood} flag when the trigger is
     * {@link Trigger#CAVE_PAINTINGS}, so {@code CavePaintings.activeEffect()}
     * grants +1 food per Artist card owned. {@code players} is expected to
     * contain exactly the owning player (see {@link DuringEvent#activeEffect});
     * other triggers are ignored.
     *
     * @param players singleton set containing the building's owner
     * @param t       the trigger that fired
     */
    @Override
    public void execute(Set<Player> players, Trigger t) {
        if (t != Trigger.CAVE_PAINTINGS) {
            return;
        }
        Optional<Player> playerContainer = players.stream().findFirst();
        if (playerContainer.isEmpty()) {
            return; // ERRORE DA GESTIRE?
        }
        Player p = playerContainer.get();
        p.getBuildingBonus().setArtistFood(true);
    }



    @Override
    protected String getEffectsLabel() {
        return CardToken.PAINTINGS + ": +1" + CardToken.FOOD + "x" + CardToken.ARTIST;
    }
}
