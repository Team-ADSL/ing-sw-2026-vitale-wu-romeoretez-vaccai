package org.adsl.server.model.cards.events;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.enums.CardType;
import org.adsl.shared.enums.Trigger;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.server.model.Player;

import java.util.Map;
import java.util.Set;

public class Hunt extends Event {

    private final int multiplierPP;

    public Hunt (String id, int multiplierPP, boolean isFinal, int era, Integer numPlayers) {
        super(id, isFinal, era, numPlayers);
        this.multiplierPP = multiplierPP;
    }

    @Override
    public void insert(Map<CardType, Set<Card>> cards) {
        if(cards.containsKey(CardType.HUNT)) cards.get(CardType.HUNT).add(this);
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

    @Override
    protected String getTypeLabel() {
        return "🐗 " + eraToRoman(getEra());
    }

    @Override
    protected String getEffectsLabel() {
        return "🌟x" + multiplierPP;
    }
}
