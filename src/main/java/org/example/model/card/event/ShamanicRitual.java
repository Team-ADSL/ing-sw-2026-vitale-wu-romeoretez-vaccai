package org.example.model.card.event;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.BuildingBonus;
import org.example.model.game.Player;

import javax.smartcardio.Card;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ShamanicRitual extends Event {

    private int lostPP;
    private int gainedPP;

    public ShamanicRitual (int lostPP, int gainedPP, boolean isFinal,
                       int era, Optional<Integer> numPlayers) {

        super(isFinal, era, numPlayers);
        this.lostPP = lostPP;
        this.gainedPP = gainedPP;

    }

    public int getLostPP() { return lostPP; }
    public int getGainedPP() { return gainedPP; }

    public void checkBuildings(Player player) {

    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if(t == Trigger.EVENT_EXECUTION) {
            boolean mostStars;
            boolean leastStars;
            for(Player p : players) {
                checkBuildings(p);
                BuildingBonus bonus = p.getBuildingBonus();
                int totalStars = p.getCards().get(CardType.SHAMAN). //conta stelle +p.getBuildingBonus().getExtraStars();
                if(leastStars & !p.getBuildingBonus().isNoRitualLostPP()){
                    p.changePP(-lostPP);
                }
                if(mostStars) {
                    p.changePP(gainedPP * p.getBuildingBonus().getShamanMulitiplierPP());
                }
                bonus.reset();
            }
        }
        // Check dei building
        // Player1 Starts = stars del player + bonusStars
        // Reset bonusBuilding
    }
}
