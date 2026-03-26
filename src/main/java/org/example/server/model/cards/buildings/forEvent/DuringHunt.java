package org.example.server.model.cards.buildings.forEvent;

import org.example.shared.enums.Trigger;
import org.example.server.model.Player;

import java.util.Optional;
import java.util.Set;

public class DuringHunt extends DuringEvent {
    public DuringHunt(int endGamePP, int cost, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, era, numPlayers);
    }

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
}
