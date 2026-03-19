package org.example.model.card.character;

import org.example.model.card.Card;

import java.util.Optional;

public abstract class Character extends Card {

    public Character (int era, Optional<Integer> numPlayers){
        super(era, numPlayers);
    }

    // MANCA IL METODO ISPICKABLE

}