package org.example.model.card.character;

import java.util.Optional;

public class Shaman extends Character {

    private int starNum;

    public Shaman(int starNum, int era, Optional<Integer> numPlayers) {
        super(era, numPlayers);
        this.starNum = starNum;
    }

    public int getStarNum() {
        return starNum;
    }
}
