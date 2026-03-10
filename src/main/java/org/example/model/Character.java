package org.example.model;

import java.util.Optional;

public abstract class Character extends Card{

public Character (int era, Optional<Integer> numPlayers){

    super(era, numPlayers);

}

}