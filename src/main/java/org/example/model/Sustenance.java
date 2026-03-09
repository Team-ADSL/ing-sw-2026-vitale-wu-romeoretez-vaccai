package org.example.model;

import java.util.Optional;

public class Sustenance extends Event {

    private int lostPP;

public Sustenance(int lostPP, boolean isFinal, int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(isFinal, id, era, numPlayers, isEvent);

    this.lostPP = lostPP;

}

}
