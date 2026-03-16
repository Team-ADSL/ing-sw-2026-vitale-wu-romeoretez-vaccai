package org.example.model.card.event;

import java.util.Optional;

public class Sustenance extends Event {

    private int lostPP;

    public Sustenance(int lostPP, boolean isFinal, int era, Optional<Integer> numPlayers) {
        super(isFinal, era, numPlayers);
        this.lostPP = lostPP;
    }

}
