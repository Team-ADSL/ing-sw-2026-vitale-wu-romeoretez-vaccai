package org.example.model;

import java.util.Optional;

public abstract class Event extends Card{

    private boolean isFinal;

public Event(boolean isFinal, int era, Optional<Integer> numPlayers) {

    super(era, numPlayers);

    this.isFinal = isFinal;

}

}
