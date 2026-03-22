package org.example.model.game.boardComponent;

import org.example.model.card.Card;

public class CardRow {
    private final Card[] cards;

    public CardRow(Card[] cards) {
        this.cards = cards;
    }
    public CardRow(int numCard) {
        this.cards = new Card[numCard];
    }


    public Card pickCardAt(int index){
        Card toReturn = cards[index];
        cards[index] = null;
        return toReturn;
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
