package org.example.server.model.cards.buildings.forEvent;

import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.server.model.Player;

import java.util.Optional;
import java.util.Set;

public class DuringSustenance extends DuringEvent {
    private final CardType typeMultiplier;

    public DuringSustenance(String id, int endGamePP, int cost, int era, Integer numPlayers, CardType typeMultiplier) {
        super(id, endGamePP, cost, era, numPlayers);
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
