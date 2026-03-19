package org.example.model.game;

import org.example.model.card.Card;

import java.util.ArrayList;

public class Deck {
    private ArrayList<Card> cards;

    public Deck(ArrayList<Card> cards) {
        this.cards = cards;
    }

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
        Card toReturn = getCards().getFirst();
        getCards().removeFirst();
        return toReturn;
    }



    public ArrayList<Card> getCards() {
        return cards;
    }
}
