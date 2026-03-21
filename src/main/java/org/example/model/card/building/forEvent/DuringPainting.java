package org.example.model.card.building.forEvent;

import org.example.model.card.Trigger;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class DuringPainting extends DuringEvent {
    public DuringPainting(int endGamePP, int cost, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, era, numPlayers);
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
}
