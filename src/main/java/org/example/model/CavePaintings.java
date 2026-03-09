package org.example.model;

import java.util.Optional;

public class CavePaintings extends Event {

    private int minArtists;
    private int lostPP;
    private int multiplierPP;

public CavePaintings (int minArtists, int lostPP, int multiplierPP, boolean isFinal, int id,
                      int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(isFinal, id, era, numPlayers, isEvent);

    this.minArtists = minArtists;
    this.lostPP = lostPP;
    this.multiplierPP = multiplierPP;

}

}
