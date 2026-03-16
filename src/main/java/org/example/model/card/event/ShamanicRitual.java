package org.example.model.card.event;

import java.util.Optional;

public class ShamanicRitual extends Event {

    private int lostPP;
    private int gainedPP;

    public ShamanicRitual (int lostPP, int gainedPP, boolean isFinal,
                       int era, Optional<Integer> numPlayers) {

        super(isFinal, era, numPlayers);
        this.lostPP = lostPP;
        this.gainedPP = gainedPP;

    }

}
