package org.example.model.card.event;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.BuildingBonus;
import org.example.model.card.character.Shaman;
import org.example.model.game.Player;

import java.util.*;

public class ShamanicRitual extends Event {

    private int lostPP;
    private int gainedPP;

    public ShamanicRitual (int lostPP, int gainedPP, boolean isFinal,
                       int era, Optional<Integer> numPlayers) {

        super(isFinal, era, numPlayers);
        this.lostPP = lostPP;
        this.gainedPP = gainedPP;

    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            Map<Player, Integer> starMap = new HashMap<>();
            for (Player p : players) {
                activateBuildings(p, Trigger.SHAMANIC_RITUAL);
                BuildingBonus bonus = p.getBuildingBonus();
                int totalStars = p.getCards().get(CardType.SHAMAN).stream()
                        .map(card -> (Shaman) card)
                        .mapToInt(Shaman::getStarNum)
                        .sum() + bonus.getExtraStars();
                starMap.put(p, totalStars);
            }
            int maxStars = Collections.max(starMap.values());
            int minStars = Collections.min(starMap.values());
            if (maxStars == minStars) {
                players.forEach(p -> p.getBuildingBonus().reset());
                return;
            }
            for (Player p : players) {
                BuildingBonus bonus = p.getBuildingBonus();
                int stars = starMap.get(p);
                if (stars == maxStars) { // DA MODIFICARE: SE E' PRESENTE UN ALTRO GIOCATORE CON MASSIMO NUMERO
                                        // DI STELLE NON DEVE RICEVERE IL BONUS
                    p.changePP(gainedPP * bonus.getShamanMulitiplierPP());
                } else if (stars == minStars) {
                    if (!bonus.isNoRitualLostPP()) {
                        p.changePP(-lostPP);
                    }
                }
                bonus.reset();
            }
        }
    }

    public int getLostPP() {
        return lostPP;
    }
    public int getGainedPP() {
        return gainedPP;
    }
}
