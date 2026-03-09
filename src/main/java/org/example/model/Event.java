package org.example.model;

import java.util.Optional;

public class Event extends Card{

    private boolean isFinal;

public Event(boolean isFinal, int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(id, era, numPlayers, isEvent);

    this.isFinal = isFinal;

}

}
