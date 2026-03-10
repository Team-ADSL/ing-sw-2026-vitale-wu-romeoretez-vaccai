package org.example.model;

import java.util.Optional;
import java.util.Set;

public class Building extends Card {

    private int endGamePP;
    private int cost;
    private Trigger trigger;
    private Set<Character> characterUse;

public Building (int endGamePP, int cost, Trigger trigger,
                 Set<Character> characterUse, int era, Optional<Integer> numPlayers) {

        super(era, numPlayers);

        this.endGamePP = endGamePP;
        this.cost = cost;
        this.trigger = trigger;
        this.characterUse = characterUse;

}

}
