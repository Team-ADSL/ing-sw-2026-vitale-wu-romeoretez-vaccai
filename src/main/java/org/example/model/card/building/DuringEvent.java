package org.example.model.card.building;

import org.example.model.card.CardType;
import org.example.model.card.Trigger;
import org.example.model.card.character.Character;
import org.example.model.game.Player;

import java.util.Optional;
import java.util.Set;

public class DuringEvent extends Building {
    public DuringEvent (int endGamePP, int cost, Trigger trigger,
                        Set<Character> characterUse, int era, Optional<Integer> numPlayers) {
        super(endGamePP, cost, trigger, characterUse, era, numPlayers);


    }

    //
    //if (trigger giusto)
        // execute

}

