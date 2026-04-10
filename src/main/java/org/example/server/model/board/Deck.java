package org.example.server.model.board;

import org.example.server.model.cards.Card;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

public record Deck(ArrayList<Card> cards) implements Serializable {
    public Deck() {
        this(new ArrayList<>());
    }

    public void addTailCard(Card card) {
        cards().addLast(card);
    }

    public void addTailCards(ArrayList<Card> cards) {
        for (Card c : cards) {
            addTailCard(c);
        }
    }

    public Card drawCard() {
        return cards.removeFirst();
    }

    public static Deck createDeck(ArrayList<Set<Card>> cards) {
        Deck deck = new Deck();
        for (Set<Card> era : cards) {
            ArrayList<Card> eraList = new ArrayList<>(era);
            Collections.shuffle(eraList);
            deck.addTailCards(eraList);
        }
        return deck;
    }

    public boolean isNewEra(int currentEra) {
        return cards.getFirst().getEra() != currentEra;
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
