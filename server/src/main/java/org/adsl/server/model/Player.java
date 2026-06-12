package org.adsl.server.model;

import org.adsl.server.model.cards.Card;
import org.adsl.server.model.cards.characters.Builder;
import org.adsl.server.model.cards.characters.Gatherer;
import org.adsl.server.model.cards.characters.Inventor;
import org.adsl.server.model.cards.characters.Shaman;
import org.adsl.shared.enums.CardType;
import org.adsl.server.model.cards.buildings.utils.BuildingBonus;
import org.adsl.shared.enums.Totem;
import org.adsl.shared.model.CardDTO;
import org.adsl.shared.model.PlayerDTO;
import java.io.IOException;
import java.io.ObjectInputStream;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Represents a player in a game session.
 * <p>
 * Stores the player's resource counters (food, prestige points), their chosen
 * totem colour, their hand of cards keyed by {@link CardType}, and transient
 * runtime state ({@code lastPick}, {@code isActive}, {@code buildingBonus}).
 * </p>
 * <p>
 * Transient fields are not serialised; {@code buildingBonus} is rebuilt in
 * {@code readObject} after deserialisation. The full-parameter constructor is
 * used only by deserialisers; new players should use the single-name constructor.
 * </p>
 */
public class Player implements Serializable {
    private final String name;
    private int food;
    private int pp;
    private Totem color;
    private final Map<CardType, Set<Card>> cards;

    private transient Card lastPick; // For SinceBuild building (see activeEffect)
    private transient boolean isActive;
    private transient BuildingBonus buildingBonus;

    /**
     * Creates a new player with default starting state: zero food, zero
     * prestige points, no totem colour, an empty hand for each {@link CardType},
     * and marked as active.
     *
     * @param name the player's username
     */
    public Player(String name) {
        this.name = name;
        this.food = 0;
        this.pp = 0;
        this.color = null;
        this.lastPick = null;
        this.isActive = true;
        this.buildingBonus = new BuildingBonus();

        this.cards = new EnumMap<>(CardType.class);
        this.cards.put(CardType.HUNTER, new HashSet<>());
        this.cards.put(CardType.GATHERER, new HashSet<>());
        this.cards.put(CardType.SHAMAN, new HashSet<>());
        this.cards.put(CardType.BUILDER, new HashSet<>());
        this.cards.put(CardType.INVENTOR, new HashSet<>());
        this.cards.put(CardType.ARTIST, new HashSet<>());
        this.cards.put(CardType.BUILDINGS, new HashSet<>());
    }

    /**
     * Reconstructs a player from persisted state. Used only by deserialisers
     * recovering a game; new players should use {@link #Player(String)}.
     * The player is initially marked inactive until reconnected.
     *
     * @param name  the player's username
     * @param food  current food tokens
     * @param pp    current prestige points
     * @param color the player's chosen totem colour, or {@code null} if not yet chosen
     * @param cards the player's hand, grouped by {@link CardType}
     */
    public Player(String name, int food, int pp, Totem color, Map<CardType, Set<Card>> cards) {
        this.name = name;
        this.food = food;
        this.pp = pp;
        this.color = color;
        this.cards = cards;
        this.lastPick = null;
        this.isActive = false;
        this.buildingBonus = new BuildingBonus();
    }

    public PlayerDTO createDTO(){
        Map<CardType, Set<CardDTO>> cardsDTO = cards.entrySet().stream()
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> e.getValue().stream()
                            .map(Card::createDTO)
                            .collect(Collectors.toSet())
            ));
        int builderPP = cardsOf(CardType.BUILDER)
                .map(c -> (Builder) c).mapToInt(Builder::getPP).sum();
        int builderDiscount = cardsOf(CardType.BUILDER)
                .map(c -> (Builder) c).mapToInt(Builder::getDiscount).sum();
        int gathererDiscount = cardsOf(CardType.GATHERER)
                .map(c -> (Gatherer) c).mapToInt(Gatherer::getDiscount).sum();
        int shamanStars = cardsOf(CardType.SHAMAN)
                .map(c -> (Shaman) c).mapToInt(Shaman::getStarNum).sum();
        int inventorUniqueIcons = (int) cardsOf(CardType.INVENTOR)
                .map(c -> (Inventor) c).map(Inventor::getIcon).distinct().count();
        return new PlayerDTO(name, food, pp, color, cardsDTO,
                builderPP, builderDiscount, gathererDiscount, shamanStars, inventorUniqueIcons);
    }

    /** Null-safe stream of the cards of a given type (the recovered-game map may be sparse). */
    private java.util.stream.Stream<Card> cardsOf(CardType type) {
        Set<Card> set = cards.get(type);
        return set == null ? java.util.stream.Stream.empty() : set.stream();
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        buildingBonus = new BuildingBonus();
    }

    /**
     * Adjusts the player's prestige points by the given delta (positive to gain,
     * negative to lose).
     *
     * @param pp the amount to add to the current prestige points
     */
    public void changePP(int pp){
        this.pp +=  pp;
    }

    /**
     * Adjusts the player's food count by the given delta (positive to gain,
     * negative to spend).
     *
     * @param food the amount to add to the current food count
     */
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
    public Totem getColor() {
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
    public void setColor(Totem color) {
        this.color = color;
    }
    public void setActive(boolean active) {
        isActive = active;
    }
}
