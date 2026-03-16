package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.card.character.Character;

import java.util.Optional;
import java.util.Set;

public class EndTurn extends Building {

    //attributes

    public EndTurn (int endGamePP, int cost, Trigger trigger,
                    Set<Character> characterUse, int era, Optional<Integer> numPlayers) {


        super(endGamePP, cost, trigger, characterUse, era, numPlayers);

    //this...

    }

}
