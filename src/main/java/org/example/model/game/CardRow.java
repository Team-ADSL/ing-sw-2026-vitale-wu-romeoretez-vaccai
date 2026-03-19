package org.example.model.game;

import org.example.model.card.Card;

import java.util.ArrayList;

public class CardRow {
    private final Card[] cards;

    public CardRow(Card[] cards) {
        this.cards = cards;
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

}
