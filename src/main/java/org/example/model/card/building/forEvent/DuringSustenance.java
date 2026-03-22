package org.example.model.card.building.forEvent;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class DuringSustenance extends DuringEvent {
    private final CardType typeMultiplier;

    public DuringSustenance(int endGamePP, int cost, int era, Optional<Integer> numPlayers, CardType typeMultiplier) {
        super(endGamePP, cost, era, numPlayers);
        this.typeMultiplier = typeMultiplier;
    }

    @Override
    public void execute(Set<Player> players, Trigger t) {
        if (t != Trigger.SUSTENANCE) {
            return;
        }
        Optional<Player> playerContainer = players.stream().findFirst();
        if (playerContainer.isEmpty()) {
            return; // ERRORE DA GESTIRE?
        }
        Player p = playerContainer.get();
        p.getBuildingBonus().setSustenanceDiscount(p.getCards().get(typeMultiplier).size());
    }

    public CardType getTypeMultiplier() {
        return typeMultiplier;
    }
}
