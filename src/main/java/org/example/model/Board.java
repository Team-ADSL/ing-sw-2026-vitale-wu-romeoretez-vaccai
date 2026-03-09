package org.example.model;

import java.util.ArrayList;
import java.util.Set;

public class Board {

    private ArrayList<Card> lowRow;
    private ArrayList<Card> upRow;
    private ArrayList<OfferTile> offerQueue;
    private ArrayList<OrderCell> orderQueue;
    private Set<Building> activeBuildings;
    private Set<Building> remainingBuildings;

public Board (ArrayList<Card> lowRow, ArrayList<Card> upRow, ArrayList<OfferTile> offerQueue,
              ArrayList<OrderCell> orderQueue, Set<Building> activeBuildings, Set<Building> remainingBuildings) {

    this.lowRow = lowRow;
    this.upRow = upRow;
    this.offerQueue = offerQueue;
    this.orderQueue = orderQueue;
    this.activeBuildings = activeBuildings;
    this.remainingBuildings = remainingBuildings;

}

}
