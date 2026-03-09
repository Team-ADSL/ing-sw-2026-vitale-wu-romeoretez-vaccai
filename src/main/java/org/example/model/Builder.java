package org.example.model;

import java.util.Optional;

public class Builder extends Card implements Charachter {

    private int discount;
    private int pp;

public Builder(int discount, int pp, int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(id, era, numPlayers, isEvent);

    this.discount = discount;
    this.pp = pp;

}

}
