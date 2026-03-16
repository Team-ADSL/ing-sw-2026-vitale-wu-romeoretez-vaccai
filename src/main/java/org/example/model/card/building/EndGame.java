package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.card.character.Character;

import java.util.Optional;
import java.util.Set;

public class EndGame extends Building {

    //attributes

    public EndGame (int endGamePP, int cost, Trigger trigger,
                    Set<Character> characterUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, trigger, characterUse, era, numPlayers);

    //this...

    }

}
