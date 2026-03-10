package org.example.model;

import java.util.Optional;

public class Inventor extends Character {

    private Icon icon;

public Inventor (Icon icon, int era, Optional<Integer> numPlayers) {

    super(era, numPlayers);

    this.icon = icon;

}

}
