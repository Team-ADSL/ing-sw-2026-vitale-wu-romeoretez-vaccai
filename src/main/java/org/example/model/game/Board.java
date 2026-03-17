package org.example.model.game;

import org.example.model.card.building.Building;
import org.example.model.card.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class Board {

    private ArrayList<Card> lowRow;
    private ArrayList<Card> topRow;
    private ArrayList<OfferTile> offerQueue;
    private ArrayList<OrderCell> orderQueue;
    private Set<Building> remainingBuildings;
    private ArrayList<Card> deck;

    public Board (ArrayList<Card> lowRow, ArrayList<Card> topRow, ArrayList<OfferTile> offerQueue,
                  ArrayList<OrderCell> orderQueue, Set<Building> activeBuildings,
                  Set<Building> remainingBuildings, ArrayList<Card> deck) {
        this.lowRow = lowRow;
        this.topRow = topRow;
        this.offerQueue = offerQueue;
        this.orderQueue = orderQueue;
        this.remainingBuildings = remainingBuildings;
        this.deck = deck;
    }

    //sets of card already sized for players number (a json for each number))
    public void makeDeck(ArrayList<Set<Card>> cards){
         this.deck = new ArrayList<>();
         //for all sets of card renamed 'era' present in 'cards'...
         for (Set<Card> era : cards) {
             //...make an arraylist to use shuffle method...
             ArrayList<Card> eraList = new ArrayList<>(era);
             //...shuffle era cards in the eraList array...
             Collections.shuffle(eraList);
             //...adding in the final deck the new shuffled eraLists in order
             this.deck.addAll(eraList);
         }
    }

    // Solo init
    public void fillTopRow(int numPlayers){
        // Calculate how many cards the upper row should contain
        int targetSize = numPlayers + 4;
        // How many cards we still need to draw
        int cardsToDraw = targetSize - topRow.size();

        for (int i = 0; i < cardsToDraw; i++) {
            // Stop drawing if the deck is empty
            if (deck.isEmpty()) {
                break;
            }
            // Draw from the top of the deck and add to the upper row
            topRow.add(deck.remove(0));
        }
    }

    // Solo init
    public void fillLowRow(){


    }

    public void makeBuildingDeck(Set<Building> buildings){
        //
    }

    public void randomPlacement(){
        //
    }

    public void placeInOrder(Player p, int arrayIndex){
        //
    }

    public void placeInOfferTile(Player p, int arrayIndex){
        //
    }

    // Aspetta
    public void changeRows(){
        //
    }

    public ArrayList<OfferTile> getOfferQueue() {
        return offerQueue;
    }

    public ArrayList<Card> getDeck() {
        return deck;
    }

    public ArrayList<Card> getLowRow() {
        return lowRow;
    }

    public ArrayList<Card> getTopRow() {
        return topRow;
    }
}
