package org.example.model.card.building.forEvent;

import org.example.model.card.Trigger;
import org.example.model.card.building.utils.BuildingEffect;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class DuringRitual extends DuringEvent {
    private final BuildingEffect buildingEffect;

    public DuringRitual(int endGamePP, int cost, int era, Optional<Integer> numPlayers, BuildingEffect buildingEffect) {
        super(endGamePP, cost, era, numPlayers);
        this.buildingEffect = buildingEffect;
    }

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
}
