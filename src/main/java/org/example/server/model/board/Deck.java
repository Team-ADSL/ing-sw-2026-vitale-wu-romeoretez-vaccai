package org.example.server.model.board;

import org.example.server.model.cards.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public class Deck {
    private ArrayList<Card> cards;

    public Deck() {
        this.cards = new ArrayList<>();
    }

    public void addTailCard(Card card){
        getCards().addLast(card);
    }

    public void addTailCards(ArrayList<Card> cards){
        for(Card c : cards){
            addTailCard(c);
        }
    }

    public Card drawCard(){
        return cards.removeFirst();
    }

    public static Deck createDeck(ArrayList<Set<Card>> cards){
        //for all sets of card renamed 'era' present in 'cards'...
        Deck deck = new Deck();
        for (Set<Card> era : cards) {
            //...make an arraylist to use shuffle method...
            ArrayList<Card> eraList = new ArrayList<>(era);
            //...shuffle era cards in the eraList array...
            Collections.shuffle(eraList);
            //...adding in the final deck the new shuffled eraLists in order
            deck.addTailCards(eraList);
        }
        return deck;
    }

    public boolean isNewEra(int currentEra){
        return cards.getFirst().getEra() != currentEra;
    }

    public ArrayList<Card> getCards() {
        return cards;
    }
    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
