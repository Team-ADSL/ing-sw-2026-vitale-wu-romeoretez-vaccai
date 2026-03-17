package org.example.model.card.event;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
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

    public int getMinArtists()   { return minArtists; }
    public int getLostPP()       { return lostPP; }
    public int getMultiplierPP() { return multiplierPP; }

    @Override
    public void activeEffect(Set<Player> players, Trigger t) {
        if (t == Trigger.CAVE_PAINTINGS) {
            for (Player p : players) {
                int artistCount = p.getCards();
                if (artistCount >= minArtists) {
                    p.changePP(artistCount * multiplierPP);
                }
                else {
                    p.changePP(-lostPP);
                }
            }
        }
    }

}
