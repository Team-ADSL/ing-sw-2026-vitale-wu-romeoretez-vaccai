package org.example.model.game;

import org.example.model.card.Card;


public class CardRow {
    private final Card[] cards;

    public CardRow(Card[] cards) {
        this.cards = cards;
    }
    public CardRow(int numCard) {
        this.cards = new Card[numCard];
    }


    public void addTailCard(Card card){
        int i = getCards().length;
        getCards()[i] = card;
    }

    public Card pickCartAt(int index){
        Card toReturn = getCards()[index];
        getCards()[index] = null;
        return toReturn;
    }

    public Card[] getCards() {
        return cards;
    }

    public int size() {
        // Count non-null slots in the array
        int count = 0;
        for (Card c : cards) {
            if (c != null) count++;
        }
        return count;
    }

    public void add(Card card) {
        // Find the first empty slot and insert the card
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) {
                cards[i] = card;
                return;
            }
        }
    }
}


