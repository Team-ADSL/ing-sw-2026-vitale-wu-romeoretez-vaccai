package org.adsl.server.model.board;

import org.adsl.server.model.cards.Card;
import org.adsl.shared.model.CardDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CardRow implements Serializable {
    private final ArrayList<Card> cards;
    private final int numTribeCards;

    public CardRow(ArrayList<Card> cards, int numTribeCards) {
        this.cards = cards;
        this.numTribeCards = numTribeCards;
    }

    public CardRow(int numCard, int numTribeCards) {
        this.cards = new ArrayList<>(numCard);
        for (int i = 0; i < numCard; i++) cards.add(null);
        this.numTribeCards = numTribeCards;
    }

    public int size(){
        return cards.size();
    }

    public Card pickCardAt(int index){
        Card toReturn = cards.get(index);
        cards.set(index, null);
        return toReturn;
    }

    public Card getCardAt(int index) {
        return cards.get(index);
    }

    public void add(Card card) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i) == null) {
                cards.set(i, card);
                return;
            }
        }
    }

    public ArrayList<Card> getTribeCards() {
        return cards.stream()
                .limit(numTribeCards)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void addTribeCards(ArrayList<Card> newCards) {
        for(int i=0; i < numTribeCards; i++){
            cards.set(i, newCards.get(i));
        }
    }

    public void clearTribeCards(){
        for (int i = 0; i < numTribeCards; i++) {
            cards.set(i, null);
        }
    }

    public Set<Card> getBuildings(){
        Set<Card> buildings = new HashSet<>();
        for (int i = numTribeCards; i < cards.size(); i++) {
            Card newCard = cards.get(i);
            if(newCard != null){
                buildings.add(newCard);
            }
        }
        return buildings;
    }

    public void clearBuildings(){
        for (int i = numTribeCards; i < cards.size(); i++) {
            cards.set(i, null);
        }
    }

    public void addBuildings(Set<Card> newCards) {
        int i = numTribeCards;
        for(Card c : newCards){
            cards.set(i, c);
            i++;
        }
    }

    public ArrayList<CardDTO> createDTO(){
        return cards.stream()
                .map(c -> c != null ? c.createDTO() : null)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public int getNumTribeCard() {
        return numTribeCards;
    }
}
