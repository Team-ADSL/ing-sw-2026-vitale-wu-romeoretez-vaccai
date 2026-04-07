package org.example.server.model;

import org.example.server.model.cards.Card;
import org.example.shared.enums.CardType;
import org.example.server.model.cards.buildings.utils.BuildingBonus;
import org.example.shared.enums.Color;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Player {
    private final String name;
    private int food;
    private int pp;
    private Color color;
    private final Map<CardType, Set<Card>> cards;
    private final BuildingBonus buildingBonus;
    private Card lastPick; // For SinceBuild building (see activeEffect)
    private boolean isActive;


    public Player(String name) {
        this.name = name;
        this.food = 0;
        this.pp = 0;
        this.color = null;
        this.lastPick = null;
        this.isActive = true;
        this.buildingBonus = new BuildingBonus(0, 1, 1, 0, false, false, false, false, false);

        this.cards = new HashMap<>();
        this.cards.put(CardType.HUNTER, new HashSet<>());
        this.cards.put(CardType.GATHERER, new HashSet<>());
        this.cards.put(CardType.SHAMAN, new HashSet<>());
        this.cards.put(CardType.BUILDER, new HashSet<>());
        this.cards.put(CardType.INVENTOR, new HashSet<>());
        this.cards.put(CardType.ARTIST, new HashSet<>());
        this.cards.put(CardType.BUILDINGS, new HashSet<>());
    }

    public Player(String name, int food, int pp, Color color, Map<CardType, Set<Card>> cards) {
        this.name = name;
        this.food = food;
        this.pp = pp;
        this.color = color;
        this.cards = cards;
        this.lastPick = null;
        this.isActive = false;
        this.buildingBonus = new BuildingBonus(0, 1, 1, 0, false, false, false, false, false);
    }

    public void changePP(int pp){
        this.pp +=  pp;
    }

    public void changeFood(int food){
        this.food += food;
    }

    public Map<CardType, Set<Card>> getCards() {
        return cards;
    }
    public BuildingBonus getBuildingBonus() {
        return buildingBonus;
    }
    public int getFood() {
        return food;
    }
    public Card getLastPick() {
        return lastPick;
    }
    public Color getColor() {
        return color;
    }
    public int getPp() {
        return pp;
    }
    public String getName() {
        return name;
    }
    public boolean isActive() {
        return isActive;
    }

    public void setLastPick(Card lastPick) {
        this.lastPick = lastPick;
    }
    public void setColor(Color color) {
        this.color = color;
    }
    public void setActive(boolean active) {
        isActive = active;
    }
}
