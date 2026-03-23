package org.example.model.game.boardComponent;
import org.example.model.card.Card;
import java.util.HashSet;
import java.util.Set;

public class CardRow {
    private final Card[] cards;
    private int nonBuildingCard;

    public CardRow(Card[] cards, int nonBuildingCard) {
        this.cards = cards;
        this.nonBuildingCard = nonBuildingCard;
    }

    public CardRow(int numCard, int nonBuildingCard) {
        this.cards = new Card[numCard];
        this.nonBuildingCard = nonBuildingCard;
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

    public void addCards(Card[] newCards) {
        if (nonBuildingCard >= 0) System.arraycopy(newCards, 0, cards, 0, nonBuildingCard);
    }

    public void addCardsFrom(Set<Card> newCards, int index) {
        for(Card c : newCards){

        }
    }

    public void clear(){
        for (int i = 0; i < nonBuildingCard; i++) {
            cards[i] = null;
        }
    }

    public Set<Card> extractBuilding(){
        Set<Card> buildings = new HashSet<>();
        for (int i = nonBuildingCard; i < cards.length; i++) {
            if(cards[i] != null){
                buildings.add(cards[i]);
                cards[i] = null;
            }
        }
        return buildings;
    }

    public Card[] getCards() {
        Card[] cardToReturn = new Card[cards.length];
        System.arraycopy(cards, 0, cardToReturn, 0, cards.length);
        return cardToReturn;
    }

    public int getNonBuildingCard() {
        return nonBuildingCard;
    }
}
