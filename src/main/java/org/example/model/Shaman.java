package org.example.model;

import java.util.Optional;

public class Shaman extends Character {

    private int starNUm;

public Shaman(int starNUm, int era, Optional<Integer> numPlayers) {

    super(era, numPlayers);

    this.starNUm = starNUm;

}

}
