package org.example.server.model.board;
import org.example.server.model.cards.Card;
import java.util.HashSet;
import java.util.Set;

public class CardRow {
    private final Card[] cards;
    private final int numTribeCards;

    public CardRow(Card[] cards, int numTribeCards) {
        this.cards = cards;
        this.numTribeCards = numTribeCards;
    }

    public CardRow(int numCard, int numTribeCards) {
        this.cards = new Card[numCard];
        this.numTribeCards = numTribeCards;
    }

    public int size(){
        return cards.length;
    }

    public Card pickCardAt(int index){
        Card toReturn = cards[index];
        cards[index] = null;
        return toReturn;
    }

    public void add(Card card) {
        for (int i = 0; i < cards.length; i++) {
            if (cards[i] == null) {
                cards[i] = card;
                return;
            }
        }
    }

    public Card[] getTribeCards() {
        Card[] cardToReturn = new Card[numTribeCards];
        System.arraycopy(cards, 0, cardToReturn, 0, numTribeCards);
        return cardToReturn;
    }

    public void addTribeCards(Card[] newCards) {
        System.arraycopy(newCards, 0, cards, 0, numTribeCards);
    }

    public void clearTribeCards(){
        for (int i = 0; i < numTribeCards; i++) {
            cards[i] = null;
        }
    }

    public Set<Card> getBuildings(){
        Set<Card> buildings = new HashSet<>();
        for (int i = numTribeCards; i < cards.length; i++) {
            if(cards[i] != null){
                buildings.add(cards[i]);
            }
        }
        return buildings;
    }

    public void addBuildings(Set<Card> newCards) {
        int i = numTribeCards;
        for(Card c : newCards){
            while(cards[i] != null){
                i++;
            }
            cards[i] = c;
        }
    }

    public void clearBuildings(){
        for (int i = numTribeCards; i < cards.length; i++) {
            cards[i] = null;
        }
    }

    public int getNumTribeCard() {
        return numTribeCards;
    }
}
