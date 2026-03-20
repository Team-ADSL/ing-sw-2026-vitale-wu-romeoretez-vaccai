package org.example.model.card.event;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.building.BuildingBonus;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class CavePaintings extends Event {

    private int minArtists;
    private int lostPP;
    private int multiplierPP;

    public CavePaintings (int minArtists, int lostPP, int multiplierPP,
                      boolean isFinal, int era, Optional<Integer> numPlayers) {

        super(isFinal, era, numPlayers);

        this.minArtists = minArtists;
        this.lostPP = lostPP;
        this.multiplierPP = multiplierPP;
    }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.EVENT_EXECUTION) {
            for (Player p : players) {
                activateBuildings(p, Trigger.CAVE_PAINTINGS);
                BuildingBonus bonus = p.getBuildingBonus();
                int artistCount = p.getCards().get(CardType.ARTIST).size();
                if (artistCount >= minArtists) {
                    p.changePP(artistCount * multiplierPP);
                }
                else {
                    p.changePP(-lostPP);
                }
                if (p.getBuildingBonus().isArtistFood()) {
                    p.changeFood(artistCount);
                };
                bonus.reset();
            }
        }
    }

    public int getMinArtists()   {
        return minArtists;
    }
    public int getLostPP()       {
        return lostPP;
    }
    public int getMultiplierPP() {
        return multiplierPP;
    }
}
