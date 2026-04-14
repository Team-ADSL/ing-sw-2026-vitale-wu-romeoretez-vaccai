package org.adsl.server.model.cards.events;

import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.server.model.Player;

import java.util.Set;

public class Hunt extends Event {

    private int multiplierPP;

    public Hunt (String id, int multiplierPP, boolean isFinal, int era, Integer numPlayers) {
        super(id, isFinal, era, numPlayers);
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
}
