package org.adsl.server.model.cards.buildings.forEvent;

import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.Player;
import org.adsl.shared.model.CardToken;

import java.util.Optional;
import java.util.Set;

public class DuringPaintings extends DuringEvent {
    public DuringPaintings(String id, int endGamePP, int cost, int era, Integer numPlayers) {
        super(id, endGamePP, cost, era, numPlayers);
    }

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
        return CardToken.PAINTINGS + "=>" + CardToken.FOOD + " " + CardToken.PP + getEndGamePP();
    }
}
