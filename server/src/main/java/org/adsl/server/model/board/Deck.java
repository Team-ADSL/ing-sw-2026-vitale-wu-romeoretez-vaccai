package org.adsl.server.model.board;

import org.adsl.server.model.cards.Card;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * The draw pile used during a game. Cards are ordered by era (era 1 first,
 * era 3 last, final-event cards after era 3). Within each era the order is
 * shuffled at creation time.
 */
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

    /**
     * Creates a shuffled deck from the list of era sets returned by
     * {@code BoardConfigLoader.getCards()}. Cards within each era are shuffled
     * independently before being appended.
     *
     * @param cards list of card sets ordered by era (index 0 = era 1, …)
     * @return a new {@link Deck} ready to draw from
     */
    public static Deck createDeck(ArrayList<Set<Card>> cards) {
        Deck deck = new Deck();
        for (Set<Card> cardSet : cards) {
            ArrayList<Card> eraList = new ArrayList<>(cardSet);
            Collections.shuffle(eraList);
            deck.addTailCards(eraList);
        }
        return deck;
    }

    /**
     * Returns {@code true} if the top card of the deck belongs to an era
     * higher than {@code currentEra}, signalling that an era transition should
     * occur. Only call this when the deck is non-empty.
     *
     * @param currentEra the era the game is currently in (1–3)
     * @return {@code true} if the next card is from a newer era
     */
    public boolean isNewEra(int currentEra) {
        return cards.getFirst().getEra() != currentEra;
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
