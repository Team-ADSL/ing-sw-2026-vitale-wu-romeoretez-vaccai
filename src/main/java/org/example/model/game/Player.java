package org.example.model.game;

import org.example.model.card.Card;
import org.example.model.card.CardType;

import java.util.Map;
import java.util.Set;

public class Player {
    private final String name;
    private int food;
    private int pp;
    private Color color;
    private Map<CardType, Set<Card>> cards;

    public Player(String name, int food, int pp, Color color, Map<CardType, Set<Card>> cards) {

    this.name = name;
    this.food = food;
    this.pp = pp;
    this.color = color;
    this.cards = cards;

    }

    public void changePP(int pp){
        //
    }

    public void changeFood(int food){
        //
    }

    public void calculateFinalPP(){
        //
    }

    public Map<CardType, Set<Card>> getCards() {
        return cards;
    }
}
