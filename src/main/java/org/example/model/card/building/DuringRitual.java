package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class DuringRitual extends DuringEvent{
    Building_Effect buildingEffect;

    public DuringRitual(int endGamePP, int cost, Trigger trigger, Set<Character> characterUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, trigger, characterUse, era, numPlayers);
        this.buildingEffect = buildingEffect;
    }

    @Override
    public void execute(Set<Player> players, Trigger t) {
        if (t!=Trigger.SHAMANIC_RITUAL){
            return;
        }
        for (Player p : players){
            if (buildingEffect==Building_Effect.RITUAL_IMMUNITY){
                p.getBuildingBonus().setNoRitualLostPP(true);
            }
            if (buildingEffect==Building_Effect.RITUAL_STARS_BONUS){
                p.getBuildingBonus().setExtraStars(3);
            }
            if (buildingEffect==Building_Effect.RITUAL_DOUBLE_PP){
                p.getBuildingBonus().setShamanMulitiplierPP(2);
            }
        }
    }
}
