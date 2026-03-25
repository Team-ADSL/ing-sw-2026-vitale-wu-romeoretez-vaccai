package org.example.model.cards.events;

import org.example.shared.enums.CardType;
import org.example.shared.enums.Trigger;
import org.example.model.cards.buildings.utils.BuildingBonus;
import org.example.model.Player;

import java.util.Optional;
import java.util.Set;

public class CavePaintings extends Event {

    private final int minArtists;
    private final int lostPP;
    private final int multiplierPP;

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
                }
                bonus.reset();
            }
        }
    }
}
