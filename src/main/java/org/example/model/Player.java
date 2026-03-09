package org.example.model;

import java.util.ArrayList;
import java.util.Set;

public class Player extends Game {
    private final String name;
    private int food;
    private int pp;
    private Color color;

public Player(String name, int food, int pp, Color color, int round, int turn, int era, Phase phase,
              ArrayList<Card> lowRow, ArrayList<Card> upRow, ArrayList<OfferTile> offerQueue,
              ArrayList<OrderCell> orderQueue, Set<Building> buildings, Set<Player> players) {

    super(round, turn, era, phase, lowRow, upRow,
            offerQueue, orderQueue, buildings, players);

    this.name = name;
    this.food = food;
    this.pp = pp;
    this.color = color;

}

}
