package org.example.model;

import java.util.Optional;
import java.util.Set;

public class Building extends Card {

    private int endGamePP;
    private int cost;
    private Trigger trigger;
    private Set<Charachter> characterUse;

public Building (int endGamePP, int cost, Trigger trigger, Set<Charachter> characterUse,
                 int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

        super(id, era, numPlayers, isEvent);

        this.endGamePP = endGamePP;
        this.cost = cost;
        this.trigger = trigger;
        this.characterUse = characterUse;

}

}
