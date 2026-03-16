package org.example.model.card.event;

import java.util.Optional;

public class Hunt extends Event {

    private int multiplierPP;

    public Hunt (int multiplierPP, boolean isFinal, int era, Optional<Integer> numPlayers) {

        super(isFinal, era, numPlayers);

        this.multiplierPP = multiplierPP;

    }

}
