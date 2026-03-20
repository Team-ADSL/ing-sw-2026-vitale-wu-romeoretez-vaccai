package org.example.model.card.event;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.utils.BuildingBonus;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class Hunt extends Event {

    private int multiplierPP;

    public Hunt (int multiplierPP, boolean isFinal, int era, Optional<Integer> numPlayers) {
        super(isFinal, era, numPlayers);
        this.multiplierPP = multiplierPP;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            for (Player p : players) {
                activateBuildings(p, Trigger.HUNT);
                BuildingBonus bonus = p.getBuildingBonus();
                int hunterCount = p.getCards().get(CardType.HUNTER).size();
                p.changePP(hunterCount * multiplierPP);
                p.changeFood(hunterCount);
                if(bonus.isHuntEventBonus()){
                    p.changePP(hunterCount);
                    p.changeFood(hunterCount);
                }
                bonus.reset();
            }
        }
    }

    public int getMultiplierPP() {
        return multiplierPP;
    }
}
