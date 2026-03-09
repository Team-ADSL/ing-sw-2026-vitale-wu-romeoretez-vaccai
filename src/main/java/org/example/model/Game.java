package org.example.model;

import java.util.ArrayList;
import java.util.Set;

public class Game {

    private int round;
    private int turn;
    private int era;
    private Phase phase;
    private ArrayList<Card> lowRow;
    private ArrayList<Card> upRow;
    private ArrayList<OfferTile> offerQueue;
    private ArrayList<OrderCell> orderQueue;
    private Set<Building> buildings;
    private Set<Player> players;

    public Game(int round, int turn, int era, Phase phase, ArrayList<Card> lowRow,
                ArrayList<Card> upRow, ArrayList<OfferTile> offerQueue,
                ArrayList<OrderCell> orderQueue, Set<Building> buildings, Set<Player> players) {

        this.round = round;
        this.turn = turn;
        this.era = era;
        this.phase = phase;
        this.lowRow = lowRow;
        this.upRow = upRow;
        this.offerQueue = offerQueue;
        this.orderQueue = orderQueue;
        this.buildings = buildings;
        this.players = players;

    }

}
