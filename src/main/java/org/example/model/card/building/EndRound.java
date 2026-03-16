package org.example.model.card.building;

import org.example.model.card.Trigger;
import org.example.model.card.character.Character;

import java.util.Optional;
import java.util.Set;

public class EndRound extends Building {

//attributes

    public EndRound (int endGamePP, int cost, Trigger trigger,
                     Set<Character> characterUse, int era, Optional<Integer> numPlayers) {

        super(endGamePP, cost, trigger, characterUse, era, numPlayers);

    //this...

    //qui ci serve un metodo che, come fa placeInOfferTile, invia una action C al controller
    //Bisogna cambiare anche Phase? Come la gestiamo?

    }

}
