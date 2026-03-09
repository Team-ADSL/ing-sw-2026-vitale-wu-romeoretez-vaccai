package org.example.model;

import java.util.Optional;

public class Inventor extends Card implements Charachter {

    private Icon icon;

public Inventor (Icon icon, int id, int era, Optional<Integer> numPlayers, boolean isEvent) {

    super(id, era, numPlayers, isEvent);

    this.icon = icon;

}

}
