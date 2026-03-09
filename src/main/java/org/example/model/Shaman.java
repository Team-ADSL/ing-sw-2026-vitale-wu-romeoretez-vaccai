package org.example.model;

import java.util.Optional;

public class Shaman extends Card implements Charachter {

    private int starNUm;

public Shaman(int starNUm, int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(id, era, numPlayers, isEvent);

    this.starNUm = starNUm;

}

}
