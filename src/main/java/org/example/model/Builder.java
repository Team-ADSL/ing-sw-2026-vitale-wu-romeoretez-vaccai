package org.example.model;

import java.util.Optional;

public class Builder extends Character {

    private int discount;
    private int pp;

public Builder(int discount, int pp, int era, Optional<Integer> numPlayers) {

    super(era, numPlayers);

    this.discount = discount;
    this.pp = pp;

}

}
